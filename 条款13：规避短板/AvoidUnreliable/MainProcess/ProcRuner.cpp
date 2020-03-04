#include "StdAfx.h"
#include "ProcRuner.h"
#include "Log.h"

#define ASIZE(a) ( sizeof(a)/sizeof(a[0]))

ProcRuner::ProcRuner(void)
{
	m_FileMapSize = 0x400000; //默认4M，在派生类中可以修改。注意需要与 exe 中定义一致
	m_WaitTimeOut = -1; //等待工作进程的时间，单位为秒。如果到时间还没执行完就强制结束工作进程。负数表示永远等待
	m_pMem = NULL;
	m_hShareMem = NULL;
}

ProcRuner::~ProcRuner(void)
{
	if(m_pMem){
		UnmapViewOfFile(m_pMem);
	}
	if(m_hShareMem){
		CloseHandle(m_hShareMem);
	}
}

CString ProcRuner::GetRandomStr()
{
	srand( (unsigned)time( NULL ) );
	int randI = rand();
	TCHAR buf[64] = {0};
	_sntprintf(buf, ASIZE(buf), _T("%d"), randI);
	return buf;
}

unsigned int ProcRuner::SerialNo = 0;
CString ProcRuner::GetSerialNo()
{
	TCHAR buf[64] = {0};
	_sntprintf(buf, ASIZE(buf), _T("%u"), SerialNo);
	SerialNo++;
	return buf;
}

BOOL ProcRuner::PrepareShareMem(CString& strMapName)
{
	strMapName = _T("Global\\") + GetSerialNo() + m_MapNameBase;
	m_hShareMem = CreateFileMapping(INVALID_HANDLE_VALUE, NULL, PAGE_READWRITE | SEC_COMMIT, 0, m_FileMapSize, strMapName);
	if( ! m_hShareMem){
		logstr("CreateFileMapping fail. error=%d, map name:%s", GetLastError(), &WToA(strMapName));
		return FALSE;
	}
	
	m_pMem = (LPBYTE)MapViewOfFile(m_hShareMem, FILE_MAP_WRITE, 0, 0, 0);
	if( ! m_pMem){
		logstr("MapViewOfFile fail. error=%d. map name:%s", GetLastError(), &WToA(strMapName));
		CloseHandle(m_hShareMem);
		m_hShareMem = NULL;
		return FALSE;
	}
	
	return TRUE;
}

HRESULT ProcRuner::GetSoftDirectory(CString &strDir)
{
	TCHAR szAppPath[MAX_PATH] = { 0 };
	
	GetModuleFileName(NULL, szAppPath, MAX_PATH);
	strDir = szAppPath;
	int pos = strDir.ReverseFind(_T('\\'));
	if(pos >= 0){
		strDir = strDir.Left(pos+1);
	}

	return S_OK;
}

int ProcRuner::CreateProcessAndWaitExit(CString strExe, CString strPar)
{
	PROCESS_INFORMATION pi;
	STARTUPINFO si;
	memset(&si,0,sizeof(si));
	si.cb=sizeof(si);
	si.wShowWindow=SW_HIDE;
	si.dwFlags=STARTF_USESHOWWINDOW;
	BOOL fRet=CreateProcess(strExe.GetBuffer(strExe.GetLength()), strPar.GetBuffer(strPar.GetLength()),NULL,FALSE,NULL, NORMAL_PRIORITY_CLASS   |   CREATE_NO_WINDOW,NULL,NULL,&si,&pi);
	
	if( ! fRet){
		int err = GetLastError();
		logstr("CreateProcess fail. error=%d path:%s", err, &WToA(strExe));
		return -err;
	}

	///判断
	DWORD   ExitCode;  
	ExitCode=STILL_ACTIVE;
	Sleep(0);
	GetExitCodeProcess(pi.hProcess,&ExitCode);

	if( m_WaitTimeOut >= 0){
		int waitCnt = m_WaitTimeOut*100;
		for(int i = 0; ExitCode==STILL_ACTIVE && i < waitCnt; i++) //按 m_WaitTimeOut 等待等待exe执行完毕
		{
			Sleep(10);
			GetExitCodeProcess(pi.hProcess,&ExitCode);
		}
	}
	else{
		for(int i = 0; ExitCode==STILL_ACTIVE; i++) //永远等待
		{
			Sleep(10);
			GetExitCodeProcess(pi.hProcess,&ExitCode);
		}
	}
	
	if(ExitCode==STILL_ACTIVE){
		//程序还未推出，强制kill
		TerminateProcess(pi.hProcess, 0);
		logstr("execute timeout. %s", &WToA(strPar));
	}
	CloseHandle(pi.hProcess);
	if(pi.hThread){
		CloseHandle(pi.hThread);
	}
	return 0;
}

int ProcRuner::Execute(CString strParam)
{
	CString strMapName;
	if( ! PrepareShareMem(strMapName)){
		logstr("PrepareShareMem fail. strParam:%s", &WToA(strParam));
		return -1;
	}

	BeforeExecute();

	CString strMapSize;
	strMapSize.Format(_T("%d"), m_FileMapSize);

	CString strCurDir;
	GetSoftDirectory(strCurDir);
	CString strExe = strCurDir + m_ProcFileName;
	DWORD dwOldTick = GetTickCount();
	int err = CreateProcessAndWaitExit(strExe, strMapName + _T("?") + strMapSize + _T("?") + strParam);
	if(err){
		logstr("CreateProcessAndWaitExit fail. err:%d", err);
		return err;
	}
	dbgstr("%s. tick=%d", &WToA(strExe), GetTickCount()-dwOldTick);

	AfterExecute();
	return 0;
}

