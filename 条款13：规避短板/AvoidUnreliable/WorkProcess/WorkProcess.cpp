
// WorkProcess.cpp : ����Ӧ�ó��������Ϊ��
//

#include "stdafx.h"
#include "WorkProcess.h"
#include "Log.h"
#include "ShareMem.h"
#include "UnstableFunction.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#endif


// CWorkProcessApp

BEGIN_MESSAGE_MAP(CWorkProcessApp, CWinAppEx)
	ON_COMMAND(ID_HELP, &CWinApp::OnHelp)
END_MESSAGE_MAP()


// CWorkProcessApp ����

CWorkProcessApp::CWorkProcessApp()
{
	// TODO: �ڴ˴���ӹ�����룬
	// ��������Ҫ�ĳ�ʼ�������� InitInstance ��
}


// Ψһ��һ�� CWorkProcessApp ����

CWorkProcessApp theApp;

typedef struct _Parameter{
	int p1;
	int p2;
}Parameter;


typedef struct _UnstFuncShareMem{
	Parameter par;
	int ret;
	Result rst;
}UnstFuncShareMem;


// CWorkProcessApp ��ʼ��

BOOL CWorkProcessApp::InitInstance()
{
	// ���һ�������� Windows XP �ϵ�Ӧ�ó����嵥ָ��Ҫ
	// ʹ�� ComCtl32.dll �汾 6 ����߰汾�����ÿ��ӻ���ʽ��
	//����Ҫ InitCommonControlsEx()�����򣬽��޷��������ڡ�
	INITCOMMONCONTROLSEX InitCtrls;
	InitCtrls.dwSize = sizeof(InitCtrls);
	// ��������Ϊ��������Ҫ��Ӧ�ó�����ʹ�õ�
	// �����ؼ��ࡣ
	InitCtrls.dwICC = ICC_WIN95_CLASSES;
	InitCommonControlsEx(&InitCtrls);

	CWinAppEx::InitInstance();

	AfxEnableControlContainer();

	CString cmdLine = GetCommandLine();
	ShareMem sm(cmdLine);
	UnstFuncShareMem* p = (UnstFuncShareMem*) sm.GetShareMem();
	if( ! p){
		logstr("get share mem fail. cmdLine:%s", &WToA(cmdLine));
		return FALSE;
	}
	p->ret = UnstableFunction(p->par.p1, p->par.p2, &p->rst);


	// ��׼��ʼ��
	// ���δʹ����Щ���ܲ�ϣ����С
	// ���տ�ִ���ļ��Ĵ�С����Ӧ�Ƴ�����
	// ����Ҫ���ض���ʼ������
	// �������ڴ洢���õ�ע�����
	// TODO: Ӧ�ʵ��޸ĸ��ַ�����
	// �����޸�Ϊ��˾����֯��
	SetRegistryKey(_T("Ӧ�ó��������ɵı���Ӧ�ó���"));

	return FALSE;
}
