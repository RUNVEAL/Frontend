# 전국 동 경계 데이터 출처 (korea_dong.geojson, korea_sigungu.geojson, korea_sido.geojson)

전국을 법정동 우선, 법정동이 없는 지역(읍/면)은 행정동 그대로 쓰는 기준으로 합쳤다
— 동 지역은 법정동 단위로 더 잘게, 읍/면은 지금까지와 동일하게.

## 원본 2개

1. **행정동(읍/면 포함)**: [vuski/admdongkor](https://github.com/vuski/admdongkor)
   `ver20261001/HangJeongDong_ver20261001.geojson` (받은 날짜: 2026-11-03).
   전국 3,559개 읍/면/행정동. 속성 `adm_cd2`(10자리), `adm_nm`, `sgg`(5자리 시군구 코드).
2. **법정동(읍/면/리 포함)**: [국토교통부 공간정보 오픈마켓](http://data.nsdi.go.kr) 경계 shapefile,
   [gisdeveloper.co.kr](http://www.gisdeveloper.co.kr/?p=2332) 미러 `EMD_202302.zip`(2023-02 기준,
   받은 날짜: 2026-11-03). 전국 5,065개 읍/면/법정동. 좌표계 EPSG:5179(GRS80 Korea Central Belt
   2000 Unified) → `proj4`로 WGS84(EPSG:4326) 재투영. 속성 `EMD_CD`(8자리 법정동코드),
   `EMD_KOR_NM`, `EMD_ENG_NM`(euc-kr 인코딩).

## 병합 기준

시군구(5자리 코드)별로:

- 이름이 `읍`/`면`으로 끝나는 행정동 파일의 피처는 그대로 유지한다(법정동 데이터는 쓰지 않음).
- 이름이 `동`으로 끝나는 행정동 피처는, 같은 시군구에 법정동 데이터가 하나라도 있으면
  **전부 버리고** 그 시군구의 법정동(이름이 `동`으로 끝나는 것만) 피처로 통째로 교체한다
  (합치지 않고 법정동 하나당 피처 하나).
- 같은 시군구에 법정동 데이터가 아예 없으면(또는 코드 불일치로 매칭 실패하면) 행정동 피처를
  그대로 둔다 — 지금까지와 동일.

시군구 256개 중 168개가 법정동 데이터를 가지고 있었고, 행정동 1,733개가 법정동 3,219개로
교체됐다(남은 423개 동은 매칭 안 돼서 행정동 그대로 남음 — 행정동(2026-10)과 법정동(2023-02)
기준 시점이 달라서 그 사이 생긴 신설/통폐합 동이 주요 원인으로 보인다). 읍/면 1,403개는
그대로다. 다른/예상 밖 접미사는 없었다(0건).

## 가공

- 좌표계 통일(WGS84) 후 두 데이터셋을 병합한 `merged.geojson`(57.7MB, 정점 194만 개)을
  `mapshaper -simplify 20% keep-shapes`로 단순화 → `korea_dong.geojson`(11.2MB, 정점 29.3만 개).
- 속성은 `adm_cd2`(키 — 행정동은 10자리, 법정동은 8자리, 서로 다른 체계라 길이는 다르지만
  문자열 키로만 쓰이므로 문제 없음), `name`(짧은 동/읍/면 이름)만 남겼다.
- `mapshaper`가 "16,023개(8% 시도 기준) / 21,550개(20%, 최종 채택) intersection을 복구 못함"이라고
  경고했다 — 행정동·법정동 두 소스가 서로 다른 정밀도/출처라 인접 경계가 정확히 안 맞물리는
  지점이 있다는 뜻. 반투명 채우기 레이어라 실사용에서는 눈에 띄는 문제가 거의 없지만, 완전히
  고친 건 아니라는 점은 밝혀둔다.
- 완주율 계산용 "동별 전체 도로 길이" 데이터(`asan_dong_lengths.json`)는 이 전국 파일과 맞지
  않아서(아산시 전용, 행정동 기준) 삭제했다 — 완주율(%)은 서버에서 계산해서 내려주는 걸로
  방향이 바뀌어서 클라이언트에 분모 데이터를 둘 필요가 없어졌다.

## 라이선스 / 출처 표기

- vuski/admdongkor: 원본 통계청 SGIS 경계 가공, CC BY 4.0. 재배포 시 SGIS + 저장소 출처 표기 필요.
- 국토교통부 공간정보 오픈마켓(법정동 경계): 공공누리 제1유형(출처 표기) 기준.
- 지도에는 OpenStreetMap, 통계청 SGIS, 국토교통부, vuski/admdongkor 출처를 표기해야 한다
  (현재 앱에는 별도 attribution UI가 아직 없음 — MapLibre 기본 로고/정보 아이콘은 꺼둔 상태라
  다른 자리에 넣어야 한다, `docs/map-module.md` 참고).

## 시군구 경계 (korea_sigungu.geojson, 줌아웃 시 보이는 개요용 레이어)

- 출처: [southkorea/southkorea-maps](https://github.com/southkorea/southkorea-maps)
  `kostat/2018/json/skorea-municipalities-2018-geo.json` (통계청 KOSTAT 2013 census 경계 가공,
  2018년 버전, 받은 날짜: 2026-11-03). 전국 250개 시군구. 이미 WGS84, 속성 `code`(5자리 시군구
  코드) → `adm_cd2`, `name`으로 정리.
- `mapshaper -simplify 10% keep-shapes`로 단순화(18.2MB → 1.5MB) — 줌아웃 전용 레이어라 해안선
  정밀도가 중요하지 않아 과감히 단순화했다.
- **2018년 기준 데이터라 다소 오래됐다**: 그 사이 신설/통폐합된 시군구가 있다면 반영이 안 돼
  있을 수 있다(동 단위보다는 훨씬 드문 변경이라 당장 문제될 가능성은 낮지만, 갱신 시 더 최신
  출처를 찾는 게 좋다 — 통계청 SGIS 공식 소스로 교체 권장).
- fill 없이 line(테두리) + label(이름)만 — 시군구 단위 완주율이라는 개념 자체가 없어서(완주율은
  동 단위로만 의미가 있음) 색칠할 값이 없다.

## 시도 경계 (korea_sido.geojson, 더 줌아웃하면 보이는 최상위 개요 레이어)

- 출처: [southkorea/southkorea-maps](https://github.com/southkorea/southkorea-maps)
  `kostat/2018/json/skorea-provinces-2018-geo.json`(시군구와 같은 저장소·같은 2018년 버전, 받은
  날짜: 2026-11-04). 전국 17개 광역시/특별시/도/특별자치시/특별자치도. 이미 WGS84, 속성
  `code`(2자리 시도 코드) → `adm_cd2`, `name`으로 정리.
- `mapshaper -simplify 8% keep-shapes`로 단순화(7.2MB → 0.76MB).
- 시군구와 같은 2018년 데이터라 **이름이 당시 기준**이다 — 예: `강원도`(현 강원특별자치도),
  `전라북도`(현 전북특별자치도). 갱신 시 통계청 SGIS 등 최신 출처로 교체하면서 이름도 같이
  바로잡는 게 좋다.
- fill 없이 line(테두리) + label(이름)만, 색은 시군구 레이어와 같은 토큰 재사용(별도 색 없음) —
  시군구와 같은 이유(완주율 개념 없음)로.
- 줌 전환: `MapConfig.SIDO_SWITCH_ZOOM`(7.0)보다 축소하면 시도만, 그 사이(7~10)는 시군구만,
  `MapConfig.SIGUNGU_SWITCH_ZOOM`(10.0)보다 확대하면 동(법정동/행정동)만 보인다 — 전부 MapLibre
  레이어의 min/maxZoom으로 전환(앱 쪽 줌 리스너 없음).

## 갱신하려면

1. `vuski/admdongkor`의 최신 `verYYYYMMDD` 폴더에서 `HangJeongDong_*.geojson`을 받는다
   (분기별 갱신, 1·4·7·10월 1일 즈음 새 버전이 올라온다).
2. gisdeveloper.co.kr의 `admin_shp`에서 최신 `EMD_*.zip`을 받는다(비정기 갱신 — 페이지에서
   최신 파일명 확인).
3. 이 문서의 "병합 기준" 그대로 다시 처리한다(좌표계 재투영 → 시군구별 교체/유지 판단 →
   `mapshaper -simplify`).
