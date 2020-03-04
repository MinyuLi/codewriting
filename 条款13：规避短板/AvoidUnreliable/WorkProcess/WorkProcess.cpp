
// WorkProcess.cpp : 定义应用程序的类行为。
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


// CWorkProcessApp 构造

CWorkProcessApp::CWorkProcessApp()
{
	// TODO: 在此处添加构造代码，
	// 将所有重要的初始化放置在 InitInstance 中
}


// 唯一的一个 CWorkProcessApp 对象

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


// CWorkProcessApp 初始化

BOOL CWorkProcessApp::InitInstance()
{
	// 如果一个运行在 Windows XP 上的应用程序清单指定要
	// 使用 ComCtl32.dll 版本 6 或更高版本来启用可视化方式，
	//则需要 InitCommonControlsEx()。否则，将无法创建窗口。
	INITCOMMONCONTROLSEX InitCtrls;
	InitCtrls.dwSize = sizeof(InitCtrls);
	// 将它设置为包括所有要在应用程序中使用的
	// 公共控件类。
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


	// 标准初始化
	// 如果未使用这些功能并希望减小
	// 最终可执行文件的大小，则应移除下列
	// 不需要的特定初始化例程
	// 更改用于存储设置的注册表项
	// TODO: 应适当修改该字符串，
	// 例如修改为公司或组织名
	SetRegistryKey(_T("应用程序向导生成的本地应用程序"));

	return FALSE;
}
