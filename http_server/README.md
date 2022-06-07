Author: Hao Li  
Nickname: saw008  
Email: hxl1033@case.edu  
  
1. All the files are located in my home directory.

2. To start my server, use the following command
    java myServer

3. <Firefox/82.0> is the browser I tested my code.

4. My config.txt consists of 3 parameters. All of them are connected with comma, no space. 
    The first one is my port number; 
    The second one is either "True" or "False" which indicates persistent On and Off, respectively;
    The third one is timer setting, the unit is seconds.

5. IMPORTANT: I have deployed my server in background now. The following is some command that could be useful.
	To shut it down: 
		kill <PID>
	To check my PID, my PID should locate at my port #, the PID looks like <PID>/java:
		netstat -ntulp
	To restart it in background:
		nohup java myServer &

Have fun!
