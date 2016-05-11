#!/usr/bin/expect -f
set timeout -1
spawn scp ${JAR_WITH_DEPENDENCIES} ${USERNAME}@${SERVERIP}:${PATH}
expect {
    "(yes/no)?" {send "yes\n"; exp_continue}
    "*assword:" {send "${PASSWORD}\n";} 
}
expect "100%"
spawn ssh root@ryanwang.cc
expect {
    "(yes/no)?" {send "yes\n"; exp_continue}
    "*assword:" {send "${PASSWORD}\n";} 
}
expect "#"
send "kill `jps \| grep \"flyingchess\" \| cut -d \" \" -f 1`\n"
expect "#"
send "nohup java -jar ${PATH} &\r\r"  
send "exit\r"
expect eof
