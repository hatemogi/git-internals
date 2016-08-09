# Git 내부 구조를 알아보자 : 1부 - 저장소 내부 맛보기

## 0부 참고

* [미디엄 글: Git 내부 구조를 알아보자 (0) 프로젝트 소개와 예고](https://medium.com/happyprogrammer-in-jeju/git-내부-구조를-알아보자-0-프로젝트-소개와-예고-bf3a8549f439)
* [유튜브 동영상: Git 내부 구조를 알아보자 (0) 예고](https://youtu.be/DWnrsbxhuOY)

## 1부 내용

* Git 내부에 저장되는 오브젝트들의 종류
* 어떻게 쓰이는가?

## Git 저장소 디렉터리의 내용

* Git은 분산 버전 관리 시스템
* 한 프로젝트의 여러 저장소가 각각 독립적으로 완전한 데이터를 보유
  * 원격 저장소 없이 로컬 저장소만으로도 버전 관리 가능

> 지금부터 로컬에 직접 저장소를 하나 새로 만들어서 진행합니다.

## README.md 파일을 추가해 봅시다.

``` bash
$ echo '# 실험용 저장소' > README.md
$ git add README.md
```

> 이거 뭡니까? `.git/objects/8a/8363d93e61185f6df18ed61321626be514c7f4`

## Git 기본: SHA-1 해시

* Git은 데이터를 저장하기 전에 항상 체크섬을 구하고 그 기준으로 데이터를 관리
* 그 체크섬을 구하는데 SHA-1 해시를 사용
  * 160bit = 20 바이트
  * 16진수 문자열 형태로 표현 -> 40글자

> 8a8363d93e61185f6df18ed61321626be514c7f4

* 이는 온세상에서 "거의" 유일한 값 -> 고유키로 가용
* 범위를 "한 저장소 안"으로 좁히면, 앞의 몇 글자만 써도 OK!

``` bash
$ git show 8a8363d93e61185f6df18ed61321626be514c7f4
$ git show 8a8363
```

이렇듯, Git은 내부적으로 SHA1을 써서 데이터를 관리하고, 편의상 그 값의 앞부분만 줄여서 지칭하기도 합니다.

## Git 기본: Content-addressable Key-Value Storage

* Git은 내용을 주소로 활용하는 파일시스템(content-addressable filesystem)
* 사실 꽤 단순한 키-밸류 스토리지
* 어떤 데이터든 Git에 담을 수 있고,
  * Git은 그 데이터의 SHA1 해시값을 키값으로 해서 저장
  * 나중에 다시 그 SHA1 값으로 보관했던 데이터를 찾음

## Plumbing & Porcelain

* 저수준(low-level)의 기본적인 일을 처리하는 plumbing 명령어들
* 고수준(high-level)에서 사용자가 일반적으로 쓰는 porcelain 명령어들

## blob 추가

* Git은 파일등의 데이터를 이 blob 형태로 저장
* 파일명 등의 메타데이터(metadata) 없이, 바이너리 데이터 자체만 저장

* `git hash-object`라는 plumbing 명령어
  * `git add README.md`로 porcelain 명령어를 썼을 때 내부적으로 활용됨

``` bash
$ cat README.md | git hash-object --stdin
$ mv .git/objects/8a/8363d93e61185f6df18ed61321626be514c7f4 8a8363.bak
$ cat README.md | git hash-object -w --stdin
$ diff .git/objects/8a/8363d93e61185f6df18ed61321626be514c7f4 8a8363.bak
```

## blob이 저장된 파일을 열어보자

``` bash
$ ruby -rzlib -e 'print Zlib::Inflate.inflate(STDIN.read)' < .git/objects/8a/8363d93e61185f6df18ed61321626be514c7f4
blob 22# 실험용 저장소
$ ruby -rzlib -e 'p Zlib::Inflate.inflate(STDIN.read)' < .git/objects/8a/8363d93e61185f6df18ed61321626be514c7f4
"blob 22\x00# \xEC\x8B\xA4\xED\x97\x98\xEC\x9A\xA9 \xEC\xA0\x80\xEC\x9E\xA5\xEC\x86\x8C\n"
```

## Zlib

조금 전 잠깐 보인 Zlib은 무료로 공개된 압축 알고리즘이자 라이브러리로, 라이선스가 자유로워서 각종 언어의 표준 라이브러리에 들어있을 정도로 많이 쓰입니다.

http://www.zlib.net

아까 보인 루비에서는 `zlib` 라이브러리로 있고, 자바 환경에는 `java.util.zip` 패키지를 써서 활용할 수 있어요.

## Git 커밋, 브랜치, 그리고 HEAD 리뷰

먼저, Git 사용하시면서 잘 알게된 내용이실 텐데, Git 내부를 파헤치는 관점으로 다시 한 번 훑어보겠습니다. Git의 커밋 각각의 순간 프로젝트의 스냅샷을 담을 수 있지요. 커밋할 때의 전체 디렉터리 내용이 다 들어있습니다. 그리고 브랜치는 그 커밋들 중 하나를 가르키고 있는 포인터입니다.
