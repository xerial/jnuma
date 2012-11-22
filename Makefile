

SRC:=src/main/java
TARGET:=target

CC:=gcc

jniheader: $(SRC)/xerial/jnuma/NumaNative.h

compile: $(wildcard $(SRC)/xerial/jnuma/*.java)
	bin/sbt compile

native: src/main/resources/xerial/jnuma/native/libjnuma.so

$(SRC)/xerial/jnuma/NumaNative.h: $(SRC)/xerial/jnuma/NumaNative.java compile
	javah -classpath $(TARGET)/classes -o $@ xerial.jnuma.NumaNative

$(TARGET)/lib/NumaNative.o : $(SRC)/xerial/jnuma/NumaNative.c
	@mkdir -p $(@D)
	$(CC) -O2 -fPIC -m64 -I include -I $(SRC)/xerial/jnuma -c $< -o $@

$(TARGET)/lib/libjnuma.so : $(TARGET)/lib/NumaNative.o
	$(CC) -Wl -O2 -fPIC -m64  -L/usr/lib64 -lc -lnuma -o $@ $+  -shared -static-libgcc
	strip $@

src/main/resources/xerial/jnuma/native/libjnuma.so : $(TARGET)/lib/libjnuma.so
	@mkdir -p $(@D)
	cp $< $@

clean-native: 
	rm -f $(TARGET)/lib/*

