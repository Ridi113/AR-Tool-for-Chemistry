# AR-Tool-for-Chemistry
It is an android AR app recognizing handwritten chemical equations, generates 3D simulation


# Char Chokka: An android AR app recognizing handwritten chemical equations, generates 3D simulation


CharChokka-App Folder:

MenuActivity.java -> 
This is the first menu activity configuration. Depending on each appropriate button, the information is transferred to main activity by passing through intents.

DrawErr.java -> Define Point classes and DrawErr classes.It's a part that stores and draws the right. The activity that generates a menu that appears when you click information about the desired chemical expression. Inherited Recyclerview, which represents a total of three pieces of information. We can connect the wiki or video information url for the chemical formula, whether to put the 3D model on the top. This allows MainActivity to display the desired information by clicking the button. MainActivity defines the following. 

GltfActivity.java -> As part of 3D rendering, the desired 3D model of the chemical formula is received via intent in the data. Therefore, access the DB information received from the json file in the following way and render it in 3D. I have imported the open source of ARCore sceneform and refactored it to AndroidX version. ARCore scope requires plane detection first. It is to put the 3D model on the floor. Therefore, remember to do plane detection first after the activity transition. The camera automatically planes when it lights up the surrounding environment. After that, if you touch the floor once, you can put the 3D model on it.


WebActivity.java & Web2Activity.java -> Web and web2 have similar roles. As follows, if you receive the desired url in the form of webview through intent in main activity, you will be able to see it.


CharChokka_DB Folder: 

Code details explained in file: documents/admin web description.doc 

Modeling Description: 

documents/Modeling_description.doc

## Demo

![](https://github.com/Ridi113/AR-Tool-for-Chemistry/blob/main/documents/ezgif.com-gif-maker%20(1).gif)


