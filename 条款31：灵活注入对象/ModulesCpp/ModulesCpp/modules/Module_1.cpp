#include "Module_1.h"

Module_1 m1;
Module_1::Module_1(void):Module("Module1")
{
}

Module_1::~Module_1(void)
{
}

int Module_1::Read()
{
	//ʵ�� module_1 �� read 
	printf("Module_1 reading...\n");
	return 0;
}
