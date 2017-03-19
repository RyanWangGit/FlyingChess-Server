# FlyingChess - Server [![Build Status](https://travis-ci.org/RyanWangGit/FlyingChess-Server.svg?branch=master)](https://travis-ci.org/RyanWangGit/FlyingChess-Server)

A Famous Board Game Backend ([wiki](https://en.wikipedia.org/wiki/Flying_chess)). 

For Android client, see [FlyingChess](https://github.com/RyanWangGit/FlyingChess) repository.

## Notes
* Server is written in `Java` and managed by `Gradle`, which communicates with the client using `Json` and adopts  [Flying Chess Control Protocol](https://github.com/RyanWangGit/FlyingChess-Server/wiki/Flying-Chess-Control-Protocol).

* For build & deploy, check [Build and Deploy](https://github.com/RyanWangGit/FlyingChess-Server/wiki/Build-and-Deploy). 
Note that `pom.xml` which `Maven` needs is also provided but no longer maintained.

* The server reads all specific information from external configuration file(which is passed into the server on initialization), thus making it a universal ssl server model.

* There could be clients in all different forms because the server basically provides standalized `Json` strings for communications.

## License
[MIT](https://github.com/RyanWangGit/FlyingChess-Server/blob/master/LICENSE).
