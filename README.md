# FlyingChess - Server
###### By : Ryan Wang @ HUST
###### Email : wangyuxin@hust.edu.cn
[![Build Status](https://travis-ci.org/RyanWangGit/FlyingChess-Server.svg?branch=master)](https://travis-ci.org/RyanWangGit/FlyingChess-Server)

A Famous Board Game ([wiki](https://en.wikipedia.org/wiki/Flying_chess)). 

Server is written in `Java` and managed by `Maven`, which communicates with the client using `Json`,
which is documented in [Flying Chess Control Protocol](https://github.com/RyanWangGit/FlyingChess-Server/wiki/Flying-Chess-Control-Protocol).
The server reads all specific information from external configuration file(which is passed into the server on initialization), thus making
it a universal ssl server model.

There could be clients in all different forms because the server basically provides standalized `Json` strings for communications.

For now we have an Android client being developed, whose project is what we initially set the server for, check [here](https://github.com/ksymphony/FlyingChess) 

For build & deploy, check [Build and Deploy](https://github.com/RyanWangGit/FlyingChess-Server/wiki/Build-and-Deploy). 