Setting up Loglist certificate
==============================

To use Loglist plugin you'll probably need to set up your system to trust its
certificate. The problem here is that Loglist uses [Let's Encrypt][lets-encrypt]
certificate and most (if not all) of the modern JVM distributions don't include
it by default.

So, to use Loglist secure connection (and that's the only option if you want to
connect to it) is to obtain and install its certificate to your JVM storage. It
could be done like this (Linux, `bash`):

    openssl s_client -showcerts -connect loglist.net:443 </dev/null 2>/dev/null|openssl x509 -outform PEM >loglist.net.pem
    keytool -import -file ./loglist.net.pem -keystore $JAVA_HOME/lib/security/cacerts -alias loglist.net

_(Note that the default password for keystore is `changeit`.)_

Or like this (Windows, PowerShell):

    $env:PATH += ';C:\Program Files\Git\usr\bin'
    bash -c 'openssl s_client -showcerts -connect loglist.net:443 </dev/null 2>/dev/null|openssl x509 -outform PEM >loglist.net.pem'
    & $env:JAVA_HOME/bin/keytool -import -file ./loglist.net.pem -keystore $env:JAVA_HOME/lib/security/cacerts -alias loglist.net

_(Please check `$env:JAVA_HOME` if it points to a proper location before
executing this command. Also I apologize for cheating with `bash` from `git`
here, but trust me, it's actually easier this way, and you likely already have
`bash` and `openssl` installed as part of a standard Git for Windows
installation. Feel free to suggest alternatives if you wish. Sincerely yours, F.
von Never.)_

[lets-encrypt]: https://letsencrypt.org/
