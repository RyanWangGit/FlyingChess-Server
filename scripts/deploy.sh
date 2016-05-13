#!/usr/bin/expect -f
set -o errexit
set timeout -1
spawn scp $env(JAR_WITH_DEPENDENCIES) $env(USERNAME)@$env(SERVERIP):$env(JARPATH)
expect {
    "(yes/no)?" {send "yes\n"; exp_continue}
    "*assword:" {send "$env(PASSWORD)\n";} 
}
expect "100%"
spawn ssh root@ryanwang.cc
expect {
    "(yes/no)?" {send "yes\n"; exp_continue}
    "*assword:" {send "$env(PASSWORD)\n";} 
}
expect "#"
send "kill `jps \| grep \"flyingchess\" \| cut -d \" \" -f 1`\n"
expect "#"
send "nohup java -jar $env(JARPATH) &\r\r"  
send "exit\r"
expect eof
