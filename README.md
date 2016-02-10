# serialMonitor

SerialMonitor permet de visualiser et de transmettre des données par les ports série disponible sur
le PC. Il permet plus spécialement de communiquer avec des Arduinos branchés à l'ordinateur.

Plusieurs ports séries peuvent être ouvert simultanément. Plusieurs fenêtres peuvent être ouvertes, 
il est alors possible de choisir quel port est afficher dans quelle fenêtre. Il est aussi possible 
de colorer le texte en fonction du port série d'où provient le message.

## fonctionnalités
* ouverture de plusieurs ports série simultanément.
* affichage du texte des ports dans une ou plusieurs fenêtre. Chaque fenêtre pouvant accueillir plusieurs 
  ports séries.
* coloration du texte en fonction du port ayant transmit.
* et quelques autres trucs pas encore assez fonctionnel pour en parler.

## Eclipse
Pour accéder à ce projet dans Eclipse :
1 File -> import ... -> Projects from git -> Clone URI ...
2 Supprimer le projet du workspace, sans supprimer les fichiers du disque
3 File -> import ... -> Gradle project