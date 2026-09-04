# 이미지 · 동영상 파일은 S3에 저장하는데, 꼭 서버를 거쳐야 할까?

## 문제

## 고민한 선택지

## 해결

현재 구현 요약 (dev 브랜치 기준)
- `POST /image/presigned-url`이 `S3Presigner`로 10분 만료 PUT URL을 서명해서 돌려준다. 이 단계에서 S3 호출은 없다.
- 클라이언트가 그 URL로 S3에 직접 업로드하고, 업로드된 URL을 회원가입 · 프로필 수정 · 게시물 등록 요청에 문자열로 넣는다.
- 서버는 URL 문자열만 저장한다.

## 결과 · 측정

## 관련 코드
- `api-server/.../domain/image/service/ImageService.java`
- `api-server/.../domain/image/controller/ImageController.java`
- 시퀀스: [docs/USE_CASE.md](../USE_CASE.md#2-이미지-직접-업로드)
