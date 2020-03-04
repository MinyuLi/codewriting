#include <windows.h>
#include <atlstr.h>

#define assert_exit(x) do{\
	if( !(x)){\
	CStringA errMsg; \
	errMsg.Format("file:%s,line:%d assert fail.(%s)",\
			__FILE__, __LINE__, #x);\
	MessageBoxA(NULL, errMsg, "´íÎó", MB_ICONSTOP);\
	exit(0);\
	}\
}while(0)
