#include "Module_2.h"

Module_2::Module_2(void):Module("Module2")
{
}

Module_2::~Module_2(void)
{
}

int Module_2::Read()
{
	//ʵ�� module_2 �� read 
	printf("Module_2 reading...\n");
	return 0;
}

Module_2 m2;