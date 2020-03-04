#pragma once
#include <string>
using namespace std;

class Module
{
public:
	virtual int Read() = 0;
	virtual ~Module(void);
	string GetName();

protected:
	Module(const char* name);
	string m_Name;
};
