#pragma once
#include "procruner.h"

typedef struct _Result{
	int r1;
	int r2;
	int r3;
}Result;

/* call unstable function*/
class CallUnstFunc :
	public ProcRuner
{
public:
	CallUnstFunc(const int p1, const int p2, Result* pRst);
	virtual ~CallUnstFunc(void);

	virtual void BeforeExecute();
	virtual void AfterExecute();

	int GetRet();

private:
	int m_p1, m_p2; // 函数如参
	int m_ret; // 函数返回值
	Result* m_Rst; // 函数出参
};
