#pragma once

class ShareMem
{
public:
	ShareMem(const CString& cmdLine);
	virtual ~ShareMem(void);

	LPBYTE GetShareMem(void);

private:
	BOOL PrepareShareMem();
	HANDLE m_hShareMem;
	LPBYTE m_pMem;
	CString m_MapName;
	int m_FileMapSize;
	CString m_Parameter;
};
