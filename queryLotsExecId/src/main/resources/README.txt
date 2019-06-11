we need to query thousands of ExecId using "where in (...)" clause.
but this clause has a limit of 1000 items. therefore, we have to 
break a list of thousands items into multiple lists. This program
does this automatically

DbQuery.java is a general class that can query both oracle, sybase, mysql.