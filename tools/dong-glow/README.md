# dong-glow — 동별 · 단계별 빛 이미지 생성기

`docs/DONG_EXPLORATION_LAYER.md`의 안쪽 빛(`effect/dong-1~6`)을 동마다 미리 구운 이미지로 만든다.
앱은 단계에 맞는 이미지를 MapLibre `ImageSource`로 동 위에 올리기만 한다.

## 결과물

```
out/
├── manifest.json
└── {adm_cd2}/
    ├── 1.webp
    ├── ...
    └── 6.webp          # 0단계는 빛이 없어서 이미지 없음
```

- **이미지에는 빛만 들어 있다** (바깥 drop shadow + 안쪽 inner shadow 2겹). 동 면(fill)과 테두리(stroke)는 지금처럼 벡터 레이어로 그린다.
- 레이어 순서: `dong-fill`(벡터) → **빛 이미지** → `dong-line`(벡터) → 라벨
- 투명 배경 RGBA(straight alpha) WebP.
- `manifest.json`의 `dongs.{adm_cd2}.levels.{N}.corners` = `[좌상, 우상, 우하, 좌하]` 순서의 `[lon, lat]`. MapLibre `LatLngQuad`(Android) / `MLNCoordinateQuad`(iOS)에 그대로 넣는다. **단계마다 여백이 달라서 corners도 단계별로 다르다.**

## 디자인을 어떻게 맞췄나

1. `design/runveal-tokens.json`의 `effect/dong/1~6` 값(색, 알파, blur, inset, blend mode)을 그대로 읽어서 피그마 방식으로 계산한다.
   - drop shadow: 동 모양에서 나오고, 동 안쪽에는 그리지 않는다 ("show behind transparent areas" 꺼짐)
   - inner shadow: 동 바깥을 blur해서 동 안쪽만 남긴다
   - 피그마 blur 값 = 가우시안 σ × 2
2. 디자인 시안의 칠각형과 픽셀 단위로 비교해서 보정했다. 단계별 밝기 차이는 0~255 기준 대부분 5 이내 (`preview/calibration.png`: 위가 시안, 아래가 생성 결과).
3. **빛 크기는 동 크기에 비례한다.** 동의 최대 내접원 반지름을 시안 칠각형의 반지름(38.8px)에 맞춰 blur를 늘리거나 줄인다. 그래서 큰 동이든 작은 동이든 시안과 같은 비율로 빛이 찬다.
4. LINEAR_DODGE(3번째 inner shadow)는 일반 합성으로 합쳤다. 검은 바탕에서는 똑같고, 어두운 지도 바탕(#0E0E0E)에서도 차이가 거의 없다.

## 실행

```bash
pip install numpy scipy pillow shapely
python3 gen_dong_glow.py \
  ../../androidApp/core/map/src/main/assets/dong/korea_dong.geojson \
  ../../design/runveal-tokens.json \
  out --prefix 11680          # 시군구/동 코드 앞자리로 범위 제한. 빼면 전국
```

- `--levels 1-6`, `--format webp|png`
- 강남구(14개 동 × 6단계 = 84장): 약 10초, 1.5MB
- 이미지 해상도: 내접원 반지름이 약 78px이 되도록(`QUALITY = 2`) 만들고, 긴 변은 1024px로 제한한다. 빛은 흐린 그라데이션이라 확대해도 티가 거의 안 나고, 날카로운 테두리는 벡터 선이 덮는다.

## 주의

- **전국을 다 만들면 약 3만 장, 500MB 이상**이라 앱에 통째로 넣기 어렵다. 지역별로 나눠 받거나, 같은 계산을 앱에서 직접 하는 방식(blur 계산이 단순해서 Kotlin으로 옮기기 쉬움)을 검토해야 한다.
- 동마다 이미지 레이어가 하나씩 생기므로, 앱에서는 **화면에 보이는 동만** 올려야 한다.
- 바깥 빛은 옆 동 위로 살짝 번진다 (시안의 drop shadow 그대로). 5~6단계에서 가장 크다.
- 경계 데이터(`korea_dong.geojson`)가 바뀌면 다시 생성해야 한다.

## 파일

- `figma_fx.py` — 토큰 읽기, 피그마식 효과 계산
- `gen_dong_glow.py` — 동별 이미지와 manifest 생성
- `calib.py` — 시안과 비교 이미지 생성 (시안 이미지 경로를 인자로 바꿔서 사용)
- `preview.py` — 생성한 이미지를 지도처럼 합쳐 본 미리보기
