webstorm
========
Application for downloading and analyzing html pages with images from RSS feeds.

Requirements to build:
=====================
Four maven modules must be installed from repo directory

mvn install:install-file -Dfile=/path/to/monitoring/module/Monitoring-0.0.1-SNAPSHOT.jar -DgroupId=cz.vutbr.fit.monitoring -DartifactId=Monitoring -Dversion=0.0.1-SNAPSHOT -Dpackaging=jar
mvn install:install-file -Dfile=/path/to/layout/module/layout-0.0.2-20140323.111228-1.jar -DgroupId=cz.vutbr.fit.burgetr -DartifactId=layout -Dversion=0.0.2-SNAPSHOT -Dpackaging=jar
mvn install:install-file -Dfile=/path/to/cssbox/module/cssbox-4.6-20140321.123233-1.jar -DgroupId=net.sf.cssbox -DartifactId=cssbox -Dversion=4.6-SNAPSHOT -Dpackaging=jar
mvn install:install-file -Dfile=/path/to/mulan/module/mulan-1.4.0.jar -DgroupId=net.sf.mulan -DartifactId=mulan -Dversion=1.4.0 -Dpackaging=jar

Requirements to run:
===================
Running database on target monitoring server. Database should be created with attached sql script.
With last commit (64b3d76848e38137b84c34e767cb8e1b28763dc0), database must be altered:
ALTER TABLE profiling ADD COLUMN num_elements integer;
