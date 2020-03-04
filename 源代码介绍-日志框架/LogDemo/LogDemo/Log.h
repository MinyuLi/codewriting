#pragma once
#include <string>
using namespace std;


class LogWriter
{
public:
	LogWriter(int logLevel, const char* logFile, const char* codeFile, int codeLine);
	LogWriter(int logLevel, const char* logFile);

	void operator()(const char *fmt, ...);
	void LogMem(const char *fmt, const void* data, const size_t dataSize);
	void Log(int logId, int par1, int par2);
	
private:
	const char* m_logFile;
	std::string m_fmt;
	int m_logLevel;
};

#ifndef LOG_FILE
#define LOG_FILE "LogFile"
#endif


#define dbgstr LogWriter(2, LOG_FILE, __FILE__, __LINE__)
#define dbgmem(fmt, data, dataSize) LogWriter(2, LOG_FILE, __FILE__, __LINE__).LogMem(fmt, data, dataSize)
#define dbgpar(logId, par1, par2) LogWriter(2, LOG_FILE).Log((logId), (par1), (par2))

#define logstr LogWriter(1, LOG_FILE, __FILE__, __LINE__)
#define logmem(fmt, data, dataSize) LogWriter(1, LOG_FILE, __FILE__, __LINE__).LogMem(fmt, data, dataSize)
#define logpar(logId, par1, par2) LogWriter(1, LOG_FILE).Log((logId), (par1), (par2))



int GetLogSwitch();
int SetLogSwitch(int ls);

#ifdef UNICODE
class WToA		// wchar_t* -> char*
{
public:
	WToA(const wchar_t* cs)
	{
		int n = WideCharToMultiByte(CP_ACP,0,cs,-1,NULL,0,NULL,NULL);
		if(n <= 0){
			buff = NULL;
			return;
		}
		buff = new char[n+2];
		if(buff){
			memset(buff,0,n+2);
			WideCharToMultiByte(CP_ACP,0,cs,-1,buff,n,NULL,NULL);
		}
	}
	~WToA()
	{
		delete[] buff;
	}
	const char* operator &()	//注意这是成员函数重载了运算符，友元函数重载需要参数
	{
		if (NULL == buff) {
			return "";
		}
		return buff;
	}
private:
	char* buff;
};
#endif
