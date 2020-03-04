#include "StdAfx.h"
#include "ShareMem.h"
#include "Log.h"

ShareMem::ShareMem(const CString& cmdLine)
{
	//���������磺mmmmm?sssss?ppppp
	//Ϊ�ʺŷָ���3���ַ�������1��Ϊ�����ڴ����ƣ�
	//��2��Ϊ�����ڴ��С����3��Ϊ�����̴����һ����������
	m_hShareMem = NULL;
	m_pMem = NULL;
	int pos = cmdLine.Find(_T('?'));
	if(pos >= 0){
		m_MapName = cmdLine.Left(pos);
		m_Parameter = cmdLine.Mid(pos+1);
		pos = m_Parameter.Find(_T('?'));
		if(pos >= 0){
			CString mapSize = m_Parameter.Left(pos);
			m_Parameter = m_Parameter.Mid(pos+1);
			m_FileMapSize = _tstoi(mapSize);
		}
		if(m_FileMapSize <= 0){
			m_FileMapSize = 0x400000; 
		}
		PrepareShareMem();
	}
}

ShareMem::~ShareMem(void)
{
	if(m_pMem){
		UnmapViewOfFile(m_pMem);
	}
	if(m_hShareMem){
		CloseHandle(m_hShareMem);
	}
}

BOOL ShareMem::PrepareShareMem()
{
	if(m_MapName == _T("")){
		logstr("share map name is empty.");
		return FALSE;
	}
	m_hShareMem = CreateFileMapping(INVALID_HANDLE_VALUE, NULL, PAGE_READWRITE | SEC_COMMIT, 0, m_FileMapSize, m_MapName);
	if( ! m_hShareMem){
		logstr("CreateFileMapping fail. error=%d, map name:%s", GetLastError(), &WToA(m_MapName));
		return FALSE;
	}
	
	m_pMem = (LPBYTE)MapViewOfFile(m_hShareMem, FILE_MAP_WRITE, 0, 0, 0);
	if( ! m_pMem){
		logstr("MapViewOfFile fail. error=%d. map name:%s", GetLastError(), &WToA(m_MapName));
		CloseHandle(m_hShareMem);
		m_hShareMem = NULL;
		return FALSE;
	}
	
	return TRUE;
}

LPBYTE ShareMem::GetShareMem(void)
{
	return m_pMem;
}

