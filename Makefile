## This makefile provides convenient access to optional utilities. It doesn't
## actually build anything.

updateSubmodules:
	git submodule foreach --recursive \
		git submodule update --init --remote --merge

createVersionCommit:
	git commit -m "v$(shell date +'%Y.%m.%d')"
	git tag "v$(shell date +'%Y.%m.%d')"

updateGitIgnore:
	find . -type f -name '.gitignore' -exec cp .gitignore {} \;

vLinux:
	vagrant status linux | grep -q 'running' || vagrant up linux
	vagrant ssh linux
