int METH(entryPoint,main)(int argc, char** argv)
{
	if (CALL(myItf,myBoolMethod)())
		return CALL(myItf,myUInt8_tMethod)();
	else
		return -CALL(myItf,myUInt8_tMethod)();
}
