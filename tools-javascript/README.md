# Shared JavaScript formatting configuration

Here's where you'll find all the configuration files related to JavaScript
formatting.

All those formatting rules are shared across Talend's frontend teams.

## Install

### WebStorm

* Rename webstorm-settings.jar.talend to webstorm-settings.jar
* Import webstorm-settings.jar as webstorm settings
* In settings > Code Style, select 'Talend' profile
* Install [eslint plugin](https://plugins.jetbrains.com/plugin/7494-eslint) and configure it (File > settings).

* For repository which has Prettier configuration, you should execute it on file saving. Follow those [configuration steps](https://github.com/prettier/prettier/blob/master/editors/webstorm/README.md).

### VSCode

* Install Microsoft's [vscode-eslint](https://github.com/Microsoft/vscode-eslint) plugin

### Sublime Text 3

* Install [SublimeLinter](http://www.sublimelinter.com/)
* Install [SublimeLinter-eslint](https://github.com/roadhump/SublimeLinter-eslint)

### Atom

* Install [Linter](https://github.com/steelbrain/linter)
* Install [linter-eslint](https://github.com/AtomLinter/linter-eslint)

### Vim

* Install eslint globally `npm install -g eslint`
* Install [syntastic](https://github.com/scrooloose/syntastic)

## Contributing

Using another editor ?
Feel free to update this README with the install instructions.
