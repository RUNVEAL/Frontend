# 전국 동 경계 데이터 출처 (korea_dong.geojson, korea_sigungu.geojson, korea_sido.geojson)

동 경계는 **행정동만** 쓴다 (읍/면/행정동, 전국 3,559개). 법정동 데이터는 더 이상 섞지 않는다.

## 원본

- [vuski/admdongkor](https://github.com/vuski/admdongkor)
  `ver20261001/HangJeongDong_ver20261001.geojson` (받은 날짜: 2026-11-08).
  전국 3,559개 읍/면/행정동, WGS84. 속성 `adm_cd2`(10자리), `adm_nm`("서울특별시 종로구 사직동"),
  `sgg`(5자리), `sggnm`, `sido`(2자리), `sidonm`, `adm_cd`.

## 코드 규칙

- 행정동 `adm_cd2`(10자리)의 **앞 5자리 = 시군구 코드, 앞 2자리 = 시도 코드** (3,559개 전부 확인).
  다만 이 문서의 시군구·시도 경계(아래 절)는 **다른 코드 체계**(통계청 2018, 예: 종로구 `11010`,
  부산 `21`)를 쓰고 있어서 이 prefix로 바로 찾을 수 없다 — 둘을 잇는 작업은 별도로 한다
  (`docs/map/refactoring/REGION_HIERARCHY.md`).

## 가공

- 속성은 `adm_cd2`, `name`(`adm_nm` 마지막 단어)만 남겼다. `mapshaper -simplify 20% keep-shapes`,
  정밀도 6자리.
- 이전: 법정동(국토교통부 2023-02) + 행정동(admdongkor 2026-10) 병합, 5,045개, 키가 8자리/10자리
  혼합이었다. 법정동 코드(8자리)로 저장·하드코딩한 값은 이 교체로 더 이상 매칭되지 않는다.
- 완주율 계산용 "동별 전체 도로 길이" 데이터(`asan_dong_lengths.json`)는 이 전국 파일과 맞지
  않아서(아산시 전용, 행정동 기준) 삭제했다 — 완주율(%)은 서버에서 계산해서 내려주는 걸로
  방향이 바뀌어서 클라이언트에 분모 데이터를 둘 필요가 없어졌다.

## 라이선스 / 출처 표기

- vuski/admdongkor: 원본 통계청 SGIS 경계 가공, CC BY 4.0. 재배포 시 SGIS + 저장소 출처 표기 필요.
- 지도에는 OpenStreetMap, 통계청 SGIS, vuski/admdongkor 출처를 표기해야 한다 (국토교통부 공간정보
  오픈마켓의 법정동 경계는 더 이상 쓰지 않으므로 표기 대상에서 빠진다). 현재 앱에는 별도
  attribution UI가 아직 없음 — MapLibre 기본 로고/정보 아이콘은 꺼둔 상태라 다른 자리에 넣어야
  한다, `docs/map-module.md` 참고.

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
  `MapConfig.SIGUNGU_SWITCH_ZOOM`(10.0)보다 확대하면 동(행정동)만 보인다 — 전부 MapLibre
  레이어의 min/maxZoom으로 전환(앱 쪽 줌 리스너 없음).

## 갱신하려면

1. `vuski/admdongkor`의 최신 `verYYYYMMDD` 폴더에서 `HangJeongDong_*.geojson`을 받는다
   (분기별 갱신, 1·4·7·10월 1일 즈음 새 버전이 올라온다).
2. gisdeveloper.co.kr의 `admin_shp`에서 최신 `EMD_*.zip`을 받는다(비정기 갱신 — 페이지에서
   최신 파일명 확인).
3. 이 문서의 "병합 기준" 그대로 다시 처리한다(좌표계 재투영 → 시군구별 교체/유지 판단 →
   `mapshaper -simplify`).
