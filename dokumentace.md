# Dokumentace projektu
## 1. Vstupy a Konfigurace

Program je plně ovládaný přes vstupní soubory CSV umístěné v `src/main/resources`:

- **`courses.csv`**: Obsahuje seznam kurzů a jejich maximální kapacitu na jednu aktivitu (např. `Math,20`). Počet potřebných paralelních aktivit pro každý kurz je vypočítán automaticky na základě celkového počtu studentů.
- **`students.csv`**: Obsahuje id studentů a jejich preference pro jednotlivé časové sloty (např. `Student1,1,1,1,1,0`). Počet hodnot určuje celkový počet dostupných časových slotů.

## 2. Implementované Algoritmy

V programu jsou porovnávány čtyři odlišné metodiky:

### A. Barvení grafu
- Čistě strukturální přístup. Aktivity jsou brány jako uzly grafu a hrany mezi nimi představují konflikty (např. stejný kurz).
- Zajišťuje rozprostření aktivit do dostupných slotů. V této fázi se nebere ohled na preference studentů.
- **Přiřazení studentů**: Provádí se následně deterministickým algoritmem (backtracking) pro maximalizaci spokojenosti v rámci daného rozvržení.

### B. Teorie her: Aukce
- Studenti hlasují svými preferencemi pro jednotlivé sloty. Aktivity jsou rozdělovány do slotů podle celkového součtu hlasů. Tím je zajištěno, že nejžádanější časy jsou spravedlivě rozděleny mezi různé kurzy.
- Každý student podává virtuální "příhozy" na aktivity úměrně svým preferencím. Tyto příhozy jsou zpracovány globálně (od nejvyšších po nejnižší). Studenti s nejsilnějším zájmem o konkrétní slot tak vyhrávají své místo jako první, dokud se nenaplní kapacity nebo nedojde ke konfliktu v rozvrhu.

### C. AI + Deterministické přiřazení
- Hybridní přístup. AI model (Google Gemini) dostane agregovaná data o preferencích a rozhodne o optimálním rozmístění aktivit do slotů.
- Samotné přiřazení studentů do těchto slotů pak provádí přesný lokální algoritmus, což eliminuje chyby AI v přiřazování dat.

### D. AI + AI přiřazení
- AI model řeší celý problém najednou – jak rozmístění aktivit, tak konkrétní přiřazení každého studenta k aktivitě.

## 3. Kritéria porovnání

Hlavním kritériem pro objektivní porovnání rozvrhů je průměrná spokojenost studenta.
- Spokojenost je vypočítána jako součet preferencí studenta pro sloty, do kterých byl přiřazen, normalizovaný na počet kurzů a studentů.
- Program vypisuje výsledky všech metod pod sebe do konzole pro snadné porovnání efektivity.

## 4. Technické detaily

- **Jazyk**: Java 25.
- **Knihovny**: 
  - `OpenCSV` pro čtení dat.
  - `Gson` pro práci s JSON (komunikace s AI).
  - `OkHttp` pro síťovou komunikaci s Google Gemini API.
  - `Dotenv` pro načtení proměnných prostředí.
- **API**: Vyžaduje nastavenou proměnnou prostředí `GOOGLE_AI_KEY`, nebo `.env` soubor s porměnnou `API_KEY` pro funkčnost AI modulů.
