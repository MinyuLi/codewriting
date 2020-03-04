#include "StdAfx.h"
#include "UnstableFunction.h"
#include "Log.h"

int UnstableFunction(const int p1, const int p2, Result* pRst)
{
	srand((unsigned)time(NULL));
	int rv = rand();
	if( (rv % 3) == 0){
		logstr("run success!");
		pRst->r1 = p1+p2;
		pRst->r2 = p1-p2;
		pRst->r3 = p1*p2;
		
		return 0;
	}
	
	if( (rv % 3) == 1){
		logstr("memory leak.");
		for(int i = 0; i < 5000; i++){
			char* p = new char[0x00040000];
		}
		return -55;
	}

	logstr("segment fault.");
	char* p = NULL;
	*p = 'a';
	return 0;
}

