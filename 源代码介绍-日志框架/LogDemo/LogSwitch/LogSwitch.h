
// LogSwitch.h : PROJECT_NAME Ӧ�ó������ͷ�ļ�
//

#pragma once

#ifndef __AFXWIN_H__
	#error "�ڰ������ļ�֮ǰ������stdafx.h�������� PCH �ļ�"
#endif

#include "resource.h"		// ������


// CLogSwitchApp:
// �йش����ʵ�֣������ LogSwitch.cpp
//

class CLogSwitchApp : public CWinAppEx
{
public:
	CLogSwitchApp();

// ��д
	public:
	virtual BOOL InitInstance();

// ʵ��

	DECLARE_MESSAGE_MAP()
};

extern CLogSwitchApp theApp;