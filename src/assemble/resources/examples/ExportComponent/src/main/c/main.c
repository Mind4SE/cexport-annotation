//Mind generated symbols declaration

//Exported from the srv interface
#include "srv_export.h"
//required for the clt interface
#include "clt_export.h"

int main(void)
{
	//Using a symbol from "srv_export.h"
	return srv_getNumber();
}

//Defining ALL symbols from "clt_export.h"
uint8_t clt_getNumber(void)
{
	return 18;
}
