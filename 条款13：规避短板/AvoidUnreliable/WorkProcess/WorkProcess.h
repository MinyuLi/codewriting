
// WorkProcess.h : PROJECT_NAME Ӧ�ó������ͷ�ļ�
//

#pragma once

#ifndef __AFXWIN_H__
	#error "�ڰ������ļ�֮ǰ������stdafx.h�������� PCH �ļ�"
#endif

#include "resource.h"		// ������


// CWorkProcessApp:
// �йش����ʵ�֣������ WorkProcess.cpp
//

class CWorkProcessApp : public CWinAppEx
{
public:
	CWorkProcessApp();

// ��д
	public:
	virtual BOOL InitInstance();

// ʵ��

	DECLARE_MESSAGE_MAP()
};

extern CWorkProcessApp theApp;