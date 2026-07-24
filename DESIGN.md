# DESIGN.md

Show Me The Money 프론트엔드의 UI 디자인 가이드입니다. 멋쟁이사자처럼(LikeLion, https://likelion.net/) 홈페이지에서 관찰한 톤 앤 매너를 기반으로 합니다.

## 1. 컨셉

- 절제된 다크 그레이 본문(`#222222`)과 옅은 헤어라인(`#e5e5e5`) 위에, 포인트 컬러인 오렌지(`#ff6000`)를 검색/강조 요소에만 집중적으로 사용합니다.
- 따뜻한 뉴트럴(`#fcf4ee`)을 프로모션/하이라이트 영역의 배경으로 사용해 공간을 구분합니다.
- 오렌지는 "포인트"로만 쓰고, 전면적인 CTA 색상이나 시맨틱 팔레트(success/error 등)로 확장하지 않습니다. 시맨틱 색상(성공/경고/에러)은 별도로 정의해서 사용합니다.

## 2. 컬러 토큰

| 역할 | Light | 용도 |
|---|---|---|
| `--color-accent` | `#ff6000` | 검색 입력, 강조 텍스트/아이콘 등 포인트 요소 |
| `--color-foreground` | `#222222` | 기본 본문 텍스트 |
| `--color-muted` | `#a3a3a3` | 저강조 텍스트 |
| `--color-muted-secondary` | `#737373` | 보조 저강조 텍스트 |
| `--color-hairline` | `#e5e5e5` | 구분선, 얇은 보더 |
| `--color-surface-warm` | `#fcf4ee` | 프로모션/하이라이트 카드 배경 |
| `--color-border` | `#d4d4d4` | 버튼/필 형태 보더 |
| `--color-canvas` | `#ffffff` | 기본 배경 |

### 다크 모드 (파생값 — 추정치)

LikeLion 공식 사이트에서 다크 모드가 관찰되지 않아, 아래 값은 위 라이트 토큰에서 명도를 반전시켜 도출한 값입니다. 실제 브랜드 다크 모드 레퍼런스가 확보되면 교체하세요.

| 역할 | Dark | 비고 |
|---|---|---|
| `--color-accent` | `#ff7a33` | 어두운 배경에서 텍스트로 쓸 때 대비 확보용. 보더/채우기 용도라면 `#ff6000` 유지 |
| `--color-foreground` | `#f2f2f2` | |
| `--color-muted` | `#9c9c9c` | |
| `--color-muted-secondary` | `#b0b0b0` | |
| `--color-hairline` | `#2e2e2e` | |
| `--color-surface-warm` | `#2a1f18` | 따뜻한 색조 유지 |
| `--color-border` | `#3f3f3f` | |
| `--color-canvas` | `#121212` | 순검정 대신 사용 |

다크 모드는 새 역할을 만들지 말고, 위 토큰들을 `data-theme="dark"` 등으로 스왑하는 방식으로 구현합니다.

## 3. 타이포그래피

- 서체: Pretendard (LikeLion 문서 사이트에서 로드 확인됨, [SIL OFL 1.1](https://github.com/orioncactus/pretendard/blob/main/LICENSE)). 시스템 폰트로 대체하지 않습니다.

| 역할 | Size | Weight | Line height |
|---|---:|---:|---:|
| 본문 | 16px | 400 | 24px |
| 섹션 제목 | 32px | 700 | 48px |
| 검색/입력 강조 | 20px | 600 | 24px |
| 카드 제목 | 20px | 600 | 30px |

## 4. 컴포넌트 가이드

- **프로모션 타일**: 배경 `--color-surface-warm`, 텍스트 `--color-foreground`, radius `16px`, padding `40px`.
- **필(pill) 버튼 / 로그인·회원가입류 컨트롤**: 텍스트 `--color-foreground`, 보더 `1px solid --color-border`, radius `9999px`, padding `10px 16px`.
- **검색/강조 입력**: 텍스트 `--color-accent`, 20px/600. 포커스 보더는 별도 포커스링 색상(예: `#2563eb`)을 사용하고 accent와 혼용하지 않습니다.
- **그림자**: 기본적으로 `box-shadow: none`. elevation은 색/보더로 구분하고, 별도 그림자 스케일은 정의하지 않습니다.

## 5. Do / Don't

**Do**
- 오렌지(`--color-accent`)는 검색·강조 등 관찰된 용도로만 사용합니다.
- 프로모션 영역에만 `--color-surface-warm`을 사용합니다.
- 다크 모드는 토큰 스왑으로만 구현합니다.

**Don't**
- 오렌지를 전면 CTA 색상이나 시맨틱 팔레트로 확장하지 않습니다.
- 임의의 컴포넌트(토스트, 다이얼로그, 탭 등) 스타일을 근거 없이 이 팔레트에서 유추하지 않습니다.
- Pretendard 대신 시스템 폰트를 쓰고 "Pretendard"라고 표기하지 않습니다.

## 6. 적용 범위

이 문서는 디자인 토큰/가이드라인 정의까지를 범위로 하며, 프론트엔드 컴포넌트(Tailwind 설정, `globals.css`, 페이지별 스타일)에 대한 실제 적용은 후속 작업에서 진행합니다.
