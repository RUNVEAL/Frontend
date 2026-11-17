# 전국 행정구역 경계·계층 데이터 출처

> **2026-11 변경**: 동 경계(`korea_dong.geojson`)와 동별 도로 길이는 더 이상 번들에 두지 않는다.
> 서버 `index.geojson`/`index.json`(`docs/api/API.md` §3.1.2)에서 받는다 — 완주율의 분자·분모·지도
> 경계가 **같은 행정동 코드 체계**를 써야 하는데, 아래 vuski 스냅샷은 서버와 코드가 달랐다
> (광주·전남 통합 `12` vs 분리 `29`/`46`, 인천 중구 재배정 등 약 13%).
>
> 지금 번들에 남은 세 파일(`korea_sigungu.geojson`, `korea_sido.geojson`,
> `korea_region_hierarchy.json`)은 **서버가 주지 않는 자료**라 유지하되, `tools/gen_region_assets.py`로
> **서버 `index.geojson`에서 생성**해 코드 체계를 맞췄다. 갱신하려면 새 `index.geojson`으로 그
> 스크립트를 다시 돌리면 된다 — 아래 mapshaper 절차는 이제 쓰지 않는다(이력으로 남겨 둠).

`korea_dong.geojson`, `korea_sigungu.geojson`, `korea_sido.geojson`, `korea_region_hierarchy.json`은 **모두 같은 원본, 같은 코드 체계**에서 만든다. (선택: `korea_sigungu_city.geojson`)

## 원본

- [vuski/admdongkor](https://github.com/vuski/admdongkor)
  `ver20261001/HangJeongDong_ver20261001.geojson` (받은 날짜: 2026-11-08).
  전국 3,559개 읍/면/행정동, WGS84. 속성 `adm_cd2`(10자리), `adm_nm`("서울특별시 종로구 사직동"),
  `sgg`(5자리), `sggnm`, `sido`(2자리), `sidonm`, `adm_cd`.

## 코드 규칙

- 행정동 `adm_cd2`(10자리)의 **앞 5자리 = 시군구 코드, 앞 2자리 = 시도 코드** (3,559개 전부 확인)
- 행정안전부 행정구역 코드 체계. 이전 시군구·시도 asset(통계청 2018 코드, 예: 종로구 `11010`, 부산 `21`)과 다르다.

## 파일별 가공

| 파일 | 개수 | 크기 | 가공 |
|---|---|---|---|
| `korea_dong.geojson` | 행정동 3,559 | 4.3MB | 속성 `adm_cd2`, `name`(`adm_nm` 마지막 단어). `-simplify 20% keep-shapes`, 정밀도 6자리 |
| `korea_sigungu.geojson` | 시군구 256 | 1.5MB | 단순화한 행정동을 `sgg`로 합침(dissolve). 속성 `adm_cd2`(5자리), `name`, `sido` |
| `korea_sido.geojson` | 시도 16 | 0.7MB | 단순화한 행정동을 `sido`로 합침. 속성 `adm_cd2`(2자리), `name` |
| `korea_sigungu_city.geojson` (선택, 앱에는 안 넣음) | 230 | 1.5MB | 일반구를 소속 시로 합친 시군구. 그 외는 `korea_sigungu`와 같음 |
| `korea_region_hierarchy.json` | 16 / 256 / 3,559 | 160KB | 시도 → 시군구 → 행정동 목록 |

- 시군구·시도는 **행정동을 먼저 단순화한 뒤 합쳐서** 만들었다. 그래서 세 경계의 선이 정확히 겹친다 (줌 전환 때 선이 어긋나지 않음).
- 일반구 시군구 이름은 `수원시 장안구`처럼 시 이름을 붙였다.
- mapshaper 경고: "교차 3개 복구, 2개 복구 못 함" (행정동 단순화 단계).

```bash
# hjd_region_props.geojson = 원본에서 sgg, sggname, sido, sidonm, city, cityname 속성만 남긴 것
mapshaper hjd_region_props.geojson -simplify 20% keep-shapes -o simp.geojson precision=0.000001
mapshaper simp.geojson -dissolve sgg  copy-fields=sggname,sido  -rename-fields adm_cd2=sgg,name=sggname  -o korea_sigungu.geojson precision=0.000001
mapshaper simp.geojson -dissolve city copy-fields=cityname,sido -rename-fields adm_cd2=city,name=cityname -o korea_sigungu_city.geojson precision=0.000001
mapshaper simp.geojson -dissolve sido copy-fields=sidonm        -rename-fields adm_cd2=sido,name=sidonm   -o korea_sido.geojson precision=0.000001
```

## 원본 데이터 기준 주의

- 원본(2026-10-01)에서 광주광역시와 전라남도가 **`전남광주통합특별시`(`12`)** 하나로 되어 있다. 시도는 16개이고, 광주 경계는 시도 레이어에 따로 없다.
- 강원특별자치도 `51`, 전북특별자치도 `52` (이전 asset은 2018년 기준 `강원도`, `전라북도`).

## 일반구 처리

수원시 장안구처럼 일반구가 있는 시(13개 시, 39개 구)는 `korea_sigungu.geojson`/`korea_region_hierarchy.json` 모두 **구 단위**로 들어 있다(`parentCity`로 소속 시를 알 수 있음). 앱의 시군구 레이어도 구 단위를 그대로 쓴다 — 완주율 집계·목록·검색 어디에도 시 단위 집계가 없어서 구 단위를 시 단위로 합칠 이유가 없었다(`docs/map/refactoring/REGION_HIERARCHY.md` 5장).

## 라이선스 / 출처 표기

- vuski/admdongkor: 원본 통계청 SGIS 경계 가공, CC BY 4.0. 재배포 시 SGIS + 저장소 출처 표기 필요.
- 출처 표기는 **마이페이지 → 약관 및 정책 → 오픈소스 라이선스** 한 곳에 모았다. 지도 화면에는
  띄우지 않는다 — MapLibre 기본 저작자 표시도 꺼 둔 상태다(`docs/map-stack/README.md` §4).
- 2026-11 이후 앱 번들에는 vuski 데이터가 들어가지 않는다(동 경계는 서버 `index.geojson`,
  시군구·시도·계층은 그 경계에서 생성). 그래도 화면에 그리는 건 앱이라 출처는 그대로 적어 둔다.
- southkorea/southkorea-maps(이전 시군구·시도), 국토교통부(이전 법정동)는 더 이상 쓰지 않는다.

## 갱신하려면

1. `vuski/admdongkor`의 최신 `verYYYYMMDD` 폴더에서 `HangJeongDong_*.geojson`을 받는다 (분기별, 1·4·7·10월 1일 즈음).
2. 위 가공을 그대로 해서 **네 파일을 한꺼번에** 다시 만든다. 하나만 갱신하면 코드·경계가 어긋난다.
3. 행정동은 통폐합으로 코드가 바뀔 수 있다. 서버 완주율 데이터와 같은 버전으로 맞추고, 사라지거나 새로 생긴 코드 목록을 남긴다.
