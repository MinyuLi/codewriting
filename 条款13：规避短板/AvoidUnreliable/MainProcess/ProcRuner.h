#pragma once

class ProcRuner //Process runer
{
public:
	virtual ~ProcRuner(void);
	int Execute(CString parameter);

protected:
	ProcRuner(void);
	virtual void BeforeExecute() = 0; //���� �������� ǰ���ã�����������ڴ���һЩ׼������
	virtual void AfterExecute() = 0; //�������� ������ɺ���ã�����������ڴ˻�ȡ�����ڴ��еĽ��

private:
	BOOL PrepareShareMem(CString& mapName);
	CString GetRandomStr();
	CString GetSerialNo();
	HRESULT GetSoftDirectory(CString &dir);
	int CreateProcessAndWaitExit(CString procPath, CString par);

protected:
	DWORD m_FileMapSize;
	CString m_MapNameBase;
	int m_WaitTimeOut; //�ȴ� �������� ִ�е�ʱ�䣬��λ����
	LPBYTE m_pMem;
	CString m_ProcFileName;

private:
	static unsigned  int SerialNo; //�������кţ�ȫ��Ψһ
	HANDLE m_hShareMem;
};
