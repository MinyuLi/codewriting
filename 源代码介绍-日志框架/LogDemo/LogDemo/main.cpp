
#include "Log.h"
#include <Windows.h>

int main(int argc, char* argv[])
{
	logstr("begin argc=%d", argc);
	for(int i = 0; i < argc; i++){
		logstr("argv[%d]:%s", i, argv[i]);
	}

	SYSTEM_INFO si = {0};
	GetSystemInfo(&si);
	logmem("system info.", &si, sizeof(si));

	for(int i = 0; i < 600; i++){
		logpar(0x00801234+i, i, 2);
	}
	return 0;
}
