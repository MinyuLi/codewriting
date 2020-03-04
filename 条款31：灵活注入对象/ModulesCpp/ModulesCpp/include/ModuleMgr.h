#pragma once

#include <string>
#include <map>
using namespace std;

class Module;
typedef map<string, Module*> ModuleMap;
typedef pair<string, Module*> ModuleMapPair;

class ModuleMgr{
public:
	virtual ~ModuleMgr();
	static ModuleMgr* GetInst();
	int RegisterModule(Module* m); //¹© module ×¢²á
	int GetModulesCnt();
	Module* FindModule(const char* name);
	void PrintAllModules();

private:
	ModuleMgr();
	ModuleMap m_MM; //modules map
};