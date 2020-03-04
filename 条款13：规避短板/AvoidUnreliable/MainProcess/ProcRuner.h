#pragma once

class ProcRuner //Process runer
{
public:
	virtual ~ProcRuner(void);
	int Execute(CString parameter);

protected:
	ProcRuner(void);
	virtual void BeforeExecute() = 0; //运行 工作进程 前调用，派生类可以在此做一些准备工作
	virtual void AfterExecute() = 0; //工作进程 运行完成后调用，派生类可以在此获取共享内存中的结果

private:
	BOOL PrepareShareMem(CString& mapName);
	CString GetRandomStr();
	CString GetSerialNo();
	HRESULT GetSoftDirectory(CString &dir);
	int CreateProcessAndWaitExit(CString procPath, CString par);

protected:
	DWORD m_FileMapSize;
	CString m_MapNameBase;
	int m_WaitTimeOut; //等待 工作进程 执行的时间，单位：秒
	LPBYTE m_pMem;
	CString m_ProcFileName;

private:
	static unsigned  int SerialNo; //自增序列号，全局唯一
	HANDLE m_hShareMem;
};
