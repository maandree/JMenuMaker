install:
	javac -s src -d . $$(find ./src | grep \\.java)
	jar -cfm JMenuMaker.jar META-INF/MANIFEST.MF *.pdf $$(find . | grep \\.java) $$(find ./se | grep \\.class) COPYING
	rm -r se
	install -d "${DESTDIR}/usr/lib"
	install -m 755 JMenuMaker.jar "${DESTDIR}/usr/lib"

uninstall:
	unlink "${DESTDIR}/usr/lib/JMenuMaker.jar"
