#!usr/bin/python
import pexpect
import sys
import os

def send_password(expect_object):
    expect_object.expect("(yes/no)?")
    expect_object.sendline("yes")
    expect_object.expect(".ssword:")
    expect_object.sendline(password)
    return

user_name = os.getenv("USERNAME")
jar_with_dependencies = os.getenv("JAR_WITH_DEPENDENCIES")
server_ip = os.getenv("SERVERIP")
jar_path = os.getenv("JARPATH")
password = os.getenv("PASSWORD")

try:
    print('Starting transmitting file to server.')
    scp = 'scp %s %s@%s:%s' % (jar_with_dependencies, user_name, server_ip, jar_path)
    expect_object = pexpect.spawn(scp, logfile=sys.stdout)
    send_password(expect_object)
    
    expect_object.expect("100%")
    print('')
    ssh = 'ssh %s@%s'%(user_name, server_ip)
    expect_object.sendline(ssh)
    send_password(expect_object)

    expect_object.expect("#")
    expect_object.sendline("kill `jps \| grep \"flyingchess\" \| cut -d \" \" -f 1`")
    expect_object.expect("#")
    expect_object.sendline('nohup java -jar %s &\n' % jar_path)
    expect_object.sendline("exit")
except Exception, e:
    print(e)
    sys.exit(1)

sys.exit(0)