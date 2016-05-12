
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
    expect_object = pexpect.spawn("scp "
                                  + jar_with_dependencies
                                  + " " + user_name + "@" + server_ip
                                  +":" + jar_path)
    send_password(expect_object)

    # login
    expect_object.expect("100%")
    expect_object.sendline("ssh " + user_name + "@" + server_ip)
    send_password(expect_object)

    expect_object.expect("#")
    expect_object.sendline("kill `jps \| grep \"flyingchess\" \| cut -d \" \" -f 1`")
    expect_object.expect("#")
    expect_object.sendline("nohup java -jar $env(JARPATH) &\n")
    expect_object.sendline("exit")
except:
    sys.exit(1)

sys.exit(0)