#include "StdAfx.h"
#include "CallUnstFunc.h"

typedef struct _Parameter{
	int p1;
	int p2;
}Parameter;

typedef struct _UnstFuncShareMem{
	Parameter par;
	int ret;
	Result rst;
}UnstFuncShareMem;

CallUnstFunc::CallUnstFunc(const int p1, const int p2, Result* pRst)
{
	m_ProcFileName = "WorkProcess.exe";
	m_MapNameBase = "MyWorkProcShareMap0efe13"; /* 自己起一个名字 */
	m_FileMapSize = sizeof(UnstFuncShareMem);
	m_p1 = p1;
	m_p2 = p2;
	m_Rst = pRst;
}

CallUnstFunc::~CallUnstFunc(void)
{
}

void CallUnstFunc::BeforeExecute()
{
	UnstFuncShareMem* p = (UnstFuncShareMem*)m_pMem;
	if(p){
		p->par.p1 = m_p1;
		p->par.p2 = m_p2;
		p->ret = -1;
	}
}

void CallUnstFunc::AfterExecute()
{
	UnstFuncShareMem* p = (UnstFuncShareMem*)m_pMem;
	if(p){
		m_ret = p->ret;
		if(p->ret == 0){
			m_Rst->r1 = p->rst.r1;
			m_Rst->r2 = p->rst.r2;
			m_Rst->r3 = p->rst.r3;
		}
	}
}

int CallUnstFunc::GetRet()
{
	return m_ret;
}