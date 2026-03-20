import requests
from bs4 import BeautifulSoup
import json
import time
import re
import pandas as pd

def scrape_team_transport_2026_final_v2():
    base_api_url = "https://hotwheels.fandom.com/api.php"
    all_data = []
    
    # Sadece 2026 yılı taranacak
    years = [2026]
    
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }

    for year in years:
        page_title = f"{year}_Car_Culture:_Team_Transport"
        print(f">>> {year} yılı taranıyor (Markasız ve Premium kategorili)...")
        
        params = {
            "action": "parse", "page": page_title, "format": "json", "prop": "text"
        }

        try:
            response = requests.get(base_api_url, params=params, headers=headers)
            data = response.json()
            if "error" in data:
                print(f"  ❌ Sayfa bulunamadı: {year}")
                continue

            soup = BeautifulSoup(data["parse"]["text"]["*"], "html.parser")
            
            tables = soup.find_all("table")
            for table in tables:
                table_class = " ".join(table.get("class", [])).lower()
                if "navbox" in table_class or "infobox" in table_class or "dirbox" in table_class:
                    continue
                    
                current_mix = ""
                prev_h = table.find_previous(['h2', 'h3'])
                if prev_h:
                    h_text = prev_h.get_text(strip=True)
                    if "Mix" in h_text:
                        current_mix = h_text.split('[')[0].strip()

                rows = table.find_all("tr")
                if not rows: continue

                max_cols = max([sum(int(c.get('colspan', 1)) for c in r.find_all(['td', 'th'])) for r in rows] + [0])
                if max_cols == 0: continue

                grid = [[''] * max_cols for _ in range(len(rows))]
                cell_elements = [[None] * max_cols for _ in range(len(rows))]

                for r_idx, row in enumerate(rows):
                    c_idx = 0
                    for cell in row.find_all(['td', 'th']):
                        while c_idx < max_cols and grid[r_idx][c_idx] != '':
                            c_idx += 1
                        if c_idx >= max_cols: break
                            
                        rs = int(cell.get('rowspan', 1))
                        cs = int(cell.get('colspan', 1))
                        
                        for br in cell.find_all('br'):
                            br.replace_with(', ')
                            
                        text = cell.get_text(separator=" ", strip=True)
                        text = re.sub(r'\s*,\s*', ', ', text).strip(', ')
                        
                        for r in range(rs):
                            for c in range(cs):
                                if r_idx + r < len(rows) and c_idx + c < max_cols:
                                    grid[r_idx + r][c_idx + c] = text
                                    cell_elements[r_idx + r][c_idx + c] = cell
                                    
                        c_idx += cs

                col_map = {}
                data_start = -1
                
                for r_idx, grid_row in enumerate(grid[:5]):
                    temp_map = {}
                    for c_idx, val in enumerate(grid_row):
                        txt = val.lower().strip()
                        if "mix" in txt and len(txt) < 10: current_mix = txt.capitalize()
                        if "photo" in txt: temp_map["photo"] = c_idx
                        elif "casting" in txt or "model" in txt or txt == "car": temp_map["model_name"] = c_idx
                        elif "toy" in txt: temp_map["toy_num"] = c_idx
                        elif "col." in txt or "col #" in txt or "collector" in txt: temp_map["col_num"] = c_idx
                        elif "series #" in txt or "set #" in txt: temp_map["series_num"] = c_idx
                        elif "color" in txt: temp_map["color"] = c_idx
                        
                    if "model_name" in temp_map:
                        col_map = temp_map
                        data_start = r_idx + 1
                        break

                if data_start == -1 or "model_name" not in col_map:
                    continue

                groups = {}
                group_col = col_map.get("toy_num", col_map.get("series_num", 0))
                
                for r_idx in range(data_start, len(grid)):
                    row_data = grid[r_idx]
                    if not any(row_data): continue 
                    group_key = row_data[group_col]
                    if not group_key: group_key = f"row_{r_idx}"
                    if group_key not in groups: groups[group_key] = []
                    groups[group_key].append(r_idx)

                for group_key, row_indices in groups.items():
                    models, colors = [], []
                    img, series_num, col_num, toy_num = "", "", "", ""
                    
                    for r_idx in row_indices:
                        row_data = grid[r_idx]
                        m = row_data[col_map["model_name"]]
                        if m and m not in models and m.lower() not in ['casting', 'model', 'tba', 'tbd']:
                            if len(m) < 150 and "•" not in m: models.append(m)
                        if "color" in col_map:
                            c = row_data[col_map["color"]]; 
                            if c and c not in colors: colors.append(c)
                        if "series_num" in col_map and not series_num: series_num = row_data[col_map["series_num"]]
                        if "col_num" in col_map and not col_num: col_num = row_data[col_map["col_num"]]
                        if "toy_num" in col_map and not toy_num: toy_num = row_data[col_map["toy_num"]]
                        if "photo" in col_map and not img:
                            cell_el = cell_elements[r_idx][col_map["photo"]]
                            if cell_el and cell_el.find("img"):
                                img = cell_el.find("img").get("data-src") or cell_el.find("img").get("src", "")
                                img = img.split("/revision/")[0]
                                    
                    combined_model = ", ".join(models)
                    combined_color = ", ".join(colors)
                    if not combined_model: continue

                    final_series = f"Team Transport ({current_mix})" if current_mix else "Team Transport"

                    all_data.append({
                        "year": str(year),
                        "series": final_series,
                        "series_num": series_num,
                        "model_name": combined_model,
                        "color": combined_color,
                        "toy_num": toy_num,
                        "col_num": col_num,
                        "image_url": img,
                        "feature": "team_transport",
                        "scale": "1:64",
                        "category": "premium"
                    })
            time.sleep(0.1)
        except Exception as e:
            print(f"  ❌ Hata {year}: {e}")

    # CSV Kaydı
    if all_data:
        df = pd.DataFrame(all_data)
        # Brand sütunu listeden çıkarıldı, sıralama güncellendi
        column_order = ["year", "series", "series_num", "model_name", "color", "toy_num", "col_num", "image_url", "feature", "scale", "category"]
        df = df[column_order]
        
        df.to_csv("hotwheels_team_transport_2026_no_brand.csv", index=False, encoding="utf-8-sig")
        print(f"\n✅ CSV OLUŞTURULDU: hotwheels_team_transport_2026_no_brand.csv")
    
    print(f"🎉 İŞLEM BİTTİ! Toplam {len(all_data)} set çekildi.")

if __name__ == "__main__":
    scrape_team_transport_2026_final_v2()