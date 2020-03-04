#include "stdafx.h"
#include "log.h"
#include <WTypes.h>
#include <time.h>
#include <stdio.h>
#include <string>
#include <process.h>
#include <map>
using namespace std;

/************************************************************************/
typedef struct MemData_
{
	int data;
}MemData, *PMemData;
class COpShareMem
{
public:
	COpShareMem();
	~COpShareMem();

public:
	bool CreateShareMem();
	bool OpenShareMem();
	bool CreateShareMem(const char* name);
	bool OpenShareMem(const char* name);
	bool WriteShareMem(const PMemData data);
	bool ReadShareMem(PMemData data);
	void ReleaseSource();

	int GetLogSwitch();
	int SetLogSwitch(int ls);

private:
	HANDLE m_memHandle;
	PMemData m_data;
	char m_memName[MAX_PATH];
};
/************************************************************************/

static BOOL folder_exist(const char* path)
{
	WIN32_FIND_DATAA wfd;
	BOOL rValue = FALSE;
	HANDLE hFind = FindFirstFileA(path, &wfd);
	if(hFind != INVALID_HANDLE_VALUE
		&& wfd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY)
	{
		rValue = TRUE;
	}
	FindClose(hFind);
	return rValue;
}

static BOOL create_folder(const char* path)
{
	SECURITY_ATTRIBUTES attrib;
	attrib.bInheritHandle = FALSE;
	attrib.lpSecurityDescriptor = NULL;
	attrib.nLength = sizeof(SECURITY_ATTRIBUTES);
	return ::CreateDirectoryA(path, &attrib);
}

static void test_and_create_folder(const char* path)
{
	string dir = path;
	size_t pos = dir.find_last_of('\\');
	if(pos > 0){
		dir = dir.substr(0,pos);
		if( ! folder_exist(dir.c_str()) ){
			create_folder(dir.c_str());
		}
	}	
}

static int fopen_ss(FILE** fp, const char* path, const char* par)
{
	test_and_create_folder(path);
	*fp = fopen(path, par);
	return *fp == NULL ? -1 : 0;
}

static void _itoa_ss(int i, char* a, int size, int par)
{
	itoa(i, a, par);
}


static DWORD gLastTickCount = 0;

static string curtime()
{
	char tmpbuf[128];
	string time;
	_strtime( tmpbuf );
	time = tmpbuf;
	time = " " + time;
	_strdate( tmpbuf );
	time = tmpbuf + time;
	return time;
}

const int LogBufferSize = 1000;

struct LogBuffer{
	time_t tm; //记录调用logpar的时间
	int logId;
	int par1;
	int par2;
};

class LogFile
{
friend class LogFileCtrl;
public:
	~LogFile();
	int WriteStr(const char *fmt, va_list ptr) const;
	int WriteMem(const char *fmt, const void* pData, const size_t size) const;

	int WriteToBuffer(int logId, int par1, int par2);
	int DumpBuffer();

	bool GetWriteLogType(UINT& type);

private:
	LogFile(const char* logFile);
	static unsigned int __stdcall DumpThread(void * pPar);

	std::string m_logFile;
	FILE* m_fp;

	struct LogBuffer m_lb[LogBufferSize];
	int m_lbPos;
	CRITICAL_SECTION m_lbLock;
};

class LogFileCtrl
{
public:
	static LogFile* GetLogFile(const char* logFile);
	static int GetLogSwitch();
	static int SetLogSwitch(int ls);
	~LogFileCtrl();

private:
	LogFileCtrl();
	std::map<string, LogFile*> m_logFileMap;
	static CRITICAL_SECTION s_lock;
	static bool s_invalid;
	static COpShareMem gMemObj;
};


LogFile::LogFile(const char* logFile) : m_logFile(logFile)
{
	std::string filepath;
	const std::string dir = "C:\\MyLog\\";
	filepath = dir + m_logFile;
	filepath += ".txt";
	if( fopen_ss(&m_fp, filepath.c_str(), "a") != 0){

		//如果文件打不开，则改个名字再试
		for(char c = '0'; c <= '9'; c++){
			filepath = dir + m_logFile + c;
			filepath += ".txt";
			if( fopen_ss(&m_fp, filepath.c_str(), "a") == 0)
				return;
		}
		m_fp = NULL;
	}
	InitializeCriticalSection(&m_lbLock);
	m_lbPos = 0;
	_beginthreadex(NULL, 0, DumpThread, this, 0, NULL);
}

LogFile::~LogFile()
{
	DumpBuffer();
	DeleteCriticalSection(&m_lbLock);
	if(m_fp)
		fclose(m_fp);
}

int LogFile::WriteStr(const char *fmt, va_list ptr) const
{
	if(!m_fp)
		return -1;
	int ret;
	ret = vfprintf(m_fp, fmt, ptr);
	fflush(m_fp);
	return ret;
}

int LogFile::WriteMem(const char *fmt, const void* pData, const size_t size) const
{
	if(!m_fp)
		return -1;

	std::string info = fmt;
	char tmp[256];
	_itoa_ss(size, tmp, sizeof(tmp), 10);
	info += "\r\nsize=";
	info += tmp;
	info +="\r\n";

	size_t n = 1;
	unsigned char* d = (unsigned char*)pData;
	while(n <= size)
	{
		_itoa_ss(*d, tmp, sizeof(tmp), 16);
		if(*d < 0x10)
			info += "0";
		info += tmp;
		info += " ";
		if(n % 16 == 0)
			info += "\r\n";

		n++;
		d++;
	}
	int ret = fprintf(m_fp, info.c_str());
	fflush(m_fp);
	return ret;
}

int LogFile::WriteToBuffer(int logId, int par1, int par2)
{
	int err = 0;
	EnterCriticalSection(&m_lbLock);
	if(m_lbPos < LogBufferSize){
		struct LogBuffer* pLB = &m_lb[m_lbPos];
		pLB->tm = time(NULL);
		pLB->logId = logId;
		pLB->par1 = par1;
		pLB->par2 = par2;
		m_lbPos++;
	}
	else{
		//超过最大日志个数,不再写入.等待缓存
		err = -1;
	}
	LeaveCriticalSection(&m_lbLock);
	return err;
}

string TimeToString(time_t t)
{
	struct tm* timeinfo;
	timeinfo = localtime(&t);
	if( !timeinfo)
		return "";

	char szTmpBuf[1024];
	memset(szTmpBuf, 0, sizeof(szTmpBuf));
	sprintf(szTmpBuf, "%.4d-%.2d-%.2d %02d:%02d:%02d", timeinfo->tm_year + 1900, timeinfo->tm_mon + 1, timeinfo->tm_mday,
		timeinfo->tm_hour, timeinfo->tm_min, timeinfo->tm_sec);

	return string(szTmpBuf);
}

int LogFile::DumpBuffer()
{
	int writeCnt = 0;
	if(m_fp){ 
		EnterCriticalSection(&m_lbLock);
		writeCnt = m_lbPos;
		std::string info;
		char tmp[1024];
		for(int i = 0; i < m_lbPos; i++){
			struct LogBuffer* pLB = &m_lb[i];
			string st = TimeToString(pLB->tm);
			_snprintf(tmp, sizeof(tmp)-1, "\r\n[%s] logId:0x%x, par1:%d, par2:%d", st.c_str(), pLB->logId, pLB->par1, pLB->par2);
			info += tmp;
		}
		int ret = fprintf(m_fp, info.c_str());
		fflush(m_fp);
		m_lbPos = 0;
		LeaveCriticalSection(&m_lbLock);
	}
	return writeCnt;
}

unsigned int __stdcall LogFile::DumpThread(void * pPar)
{
	LogFile* pThis = (LogFile*)pPar;
	while(1){
		Sleep(2000);
		if( pThis->m_lbPos >= (LogBufferSize >> 1)){//缓存超过一半开始写入日志文件
			pThis->DumpBuffer();
		}
	}
}

CRITICAL_SECTION LogFileCtrl::s_lock;
bool LogFileCtrl::s_invalid = false;
COpShareMem LogFileCtrl::gMemObj;

LogFileCtrl::LogFileCtrl()
{
	InitializeCriticalSection(&s_lock);
}

LogFileCtrl::~LogFileCtrl()
{
	s_invalid = true;
	map<string, LogFile*>::iterator mlf;
	for(mlf = m_logFileMap.begin(); mlf != m_logFileMap.end(); mlf++)
	{
		delete mlf->second;
	}
	m_logFileMap.clear();
	DeleteCriticalSection(&s_lock);
}

LogFile* LogFileCtrl::GetLogFile(const char *logFile)
{
	static LogFileCtrl lfc;
	map<string, LogFile*>::iterator mlf;
	if(s_invalid)
		return NULL; //LogFileCtrl lfc 已经析构了，不再写入日志
	
	EnterCriticalSection(&s_lock);
	mlf = lfc.m_logFileMap.find(logFile);
	if(mlf != lfc.m_logFileMap.end()){
		LeaveCriticalSection(&s_lock);
		return mlf->second;
	}

	LogFile* lf = new LogFile(logFile);
	lfc.m_logFileMap.insert(pair<string, LogFile*>(logFile, lf));
	LeaveCriticalSection(&s_lock);
	return lf;
}

int LogFileCtrl::GetLogSwitch()
{
	if(s_invalid)
		return 0;
	return gMemObj.GetLogSwitch();
}

int LogFileCtrl::SetLogSwitch(int ls)
{
	if(s_invalid)
		return -1;
	return gMemObj.SetLogSwitch(ls);
}

LogWriter::LogWriter(int logLevel, const char* logFile, const char* codeFile, int codeLine) 
		 : m_logFile(logFile)
{
	char tmp[256];
	_itoa_ss(codeLine, tmp, sizeof(tmp), 10);
	m_fmt = codeFile;
	m_fmt = "\r\nFILE:" + m_fmt + "\r\nLINE:";
	m_fmt += tmp;
	m_fmt += "\r\n";

	m_logLevel = logLevel;
}

LogWriter::LogWriter(int logLevel, const char* logFile) 
		 : m_logFile(logFile)
{
	m_fmt += "\r\n";
	m_logLevel = logLevel;
}

static void TestAndWriteCurrentTime(const char* logFile)
{
	DWORD curTick = GetTickCount();
	if(curTick > gLastTickCount + 1000){
		string timeInfo = "\r\n\r\nCurrent time:" + curtime() + "\r\n";
		gLastTickCount = GetTickCount();
		const LogFile* lf = LogFileCtrl::GetLogFile(logFile);
		if(lf){
			lf->WriteStr(timeInfo.c_str(), 0);
		}
	}
}

bool CouldWriteLog(int lvl)
{
	UINT lType = LogFileCtrl::GetLogSwitch();
	if(lType >= lvl)
		return true;
	return false;
}

int GetLogSwitch()
{
	return LogFileCtrl::GetLogSwitch();
}

int SetLogSwitch(int ls)
{
	return LogFileCtrl::SetLogSwitch(ls);
}

void LogWriter::operator()(const char *fmt, ...)
{
	if ( ! CouldWriteLog(m_logLevel))
		return;
	TestAndWriteCurrentTime(m_logFile);
	va_list ptr; va_start(ptr, fmt);
	m_fmt += fmt;
	const LogFile* lf = LogFileCtrl::GetLogFile(m_logFile);
	if(lf){
		lf->WriteStr(m_fmt.c_str(), ptr);
	}
	va_end(ptr);
}

void LogWriter::LogMem(const char *fmt, const void* pData, const size_t size)
{
	if ( ! CouldWriteLog(m_logLevel))
		return;
	TestAndWriteCurrentTime(m_logFile);
	m_fmt += fmt;
	const LogFile* lf = LogFileCtrl::GetLogFile(m_logFile);
	if(lf){
		lf->WriteMem(m_fmt.c_str(), pData, size);
	}
}

void LogWriter::Log(int logId, int par1, int par2)
{
	if ( ! CouldWriteLog(m_logLevel))
		return;
	LogFile* lf = LogFileCtrl::GetLogFile(m_logFile);
	if(lf){
		lf->WriteToBuffer(logId, par1, par2);
	}
}


/************************************************************************/

COpShareMem::COpShareMem()
{
	strcpy(m_memName, "Global\\BCFF3605-EFA8-4F49-A363-75C2B5D61A79");
	m_memHandle = NULL;
	m_data = NULL;
	CreateShareMem();
}
COpShareMem::~COpShareMem()
{
	//ReleaseSource();
}
/* public func */
bool COpShareMem::CreateShareMem(const char* name)
{
	if (NULL != name)
	{
		strncpy(m_memName, name, sizeof(m_memName) - 1);
		m_memName[strlen(m_memName)] = '\0';
		return CreateShareMem();
	}
	return false;
}
bool COpShareMem::CreateShareMem()
{
	if (!OpenShareMem())
	{
		m_memHandle = ::CreateFileMappingA(INVALID_HANDLE_VALUE, NULL, PAGE_READWRITE | SEC_COMMIT, 0, sizeof(MemData), m_memName);
		if (!m_memHandle)
			return false;
		m_data = (PMemData)::MapViewOfFile(m_memHandle, FILE_MAP_WRITE, 0, 0, 0);
		if(m_data){
			m_data->data = 1;
		}

		return NULL == m_data ? false : true;
	}
 	else
 		return true;
}
bool COpShareMem::OpenShareMem(const char* name)
{
	if (NULL != name)
	{
		strncpy(m_memName, name, sizeof(m_memName) - 1);
		m_memName[strlen(m_memName)] = '\0';
		return OpenShareMem();
	}
	return false;
}
bool COpShareMem::OpenShareMem()
{
	ReleaseSource();
	m_memHandle = ::OpenFileMappingA(PAGE_READWRITE, false, m_memName);
	if (!m_memHandle)
		return false;
	m_data = (PMemData)::MapViewOfFile(m_memHandle, FILE_MAP_WRITE, 0, 0, 0);
	return NULL == m_data ? false : true;
}
bool COpShareMem::WriteShareMem(const PMemData data)
{
	if (m_data)
	{
		m_data->data = data->data;
		return true;
	}
	return false;
}
bool COpShareMem::ReadShareMem(PMemData data)
{
	if (m_data){
		data->data = m_data->data;
		return true;
	}
	return false;
}

int COpShareMem::GetLogSwitch()
{
	if(m_data){
		return m_data->data;
	}
	return 0;
}

int COpShareMem::SetLogSwitch(int ls)
{
	if(m_data){
		m_data->data = ls;
		return 0;
	}
	return -1;
}

void COpShareMem::ReleaseSource()
{
	if (NULL != m_data) {
		::UnmapViewOfFile(m_data);
		m_data = NULL;
	}
	if (NULL != m_memHandle) {
		::CloseHandle(m_memHandle);
		m_memHandle = NULL;
	}
}
/************************************************************************/