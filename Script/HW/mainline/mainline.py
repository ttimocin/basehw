import requests
from bs4 import BeautifulSoup
import pandas as pd
import time

def scrape_2026_mainline_v3_fixed():
    base_api_url = "https://hotwheels.fandom.com/api.php"
    page_title = "List_of_2026_Hot_Wheels"
    all_data = []
    
    headers = {"User-Agent": "Mozilla/5.0"}
    params = {"action": "parse", "page": page_title, "format": "json", "prop": "text"}

    print(f">>> {page_title} taranıyor... (Koordinat Sistemi Aktif)")

    try:
        response = requests.get(base_api_url, params=params, headers=headers)
        soup = BeautifulSoup(response.json()["parse"]["text"]["*"], "html.parser")
        tables = soup.find_all("table", class_="wikitable")

        for table in tables:
            rows = table.find_all("tr")
            if not rows: continue

            # Tabloyu bir matris olarak simüle et (Kaymaları önlemek için)
            num_rows = len(rows)
            num_cols = 15 # Güvenli genişlik
            grid = [[None for _ in range(num_cols)] for _ in range(num_rows)]
            images = [[None for _ in range(num_cols)] for _ in range(num_rows)]

            for r_idx, row in enumerate(rows):
                cells = row.find_all(["td", "th"])
                curr_c = 0
                for cell in cells:
                    # Dolu hücreyi atla
                    while curr_c < num_cols and grid[r_idx][curr_c] is not None:
                        curr_c += 1
                    
                    rowspan = int(cell.get("rowspan", 1))
                    colspan = int(cell.get("colspan", 1))
                    content = cell.get_text(strip=True)
                    
                    # Görseli al
                    img_tag = cell.find("img")
                    img_url = ""
                    if img_tag:
                        img_url = (img_tag.get("data-src") or img_tag.get("src", "")).split("/revision/")[0]

                    # Matrisi doldur
                    for r in range(rowspan):
                        for c in range(colspan):
                            if r_idx + r < num_rows and curr_c + c < num_cols:
                                grid[r_idx + r][curr_c + c] = content
                                if img_tag:
                                    images[r_idx + r][curr_c + c] = img_url
                    curr_c += colspan

            # Başlıkları ilk satırdan (grid[0]) tespit et
            header = [str(x).lower() for x in grid[0]]
            col_map = {}
            for i, h in enumerate(header):
                if "model" in h or "casting" in h: col_map["model"] = i
                elif "toy" in h: col_map["toy"] = i
                elif "col" in h and "color" not in h: col_map["col"] = i
                elif "series" in h and "#" in h: col_map["s_num"] = i
                elif "series" in h and "#" not in h: col_map["s_name"] = i
                elif "color" in h: col_map["color"] = i
                elif "photo" in h: col_map["photo"] = i

            if "model" not in col_map: continue

            # Verileri satır satır işle
            for r in range(1, num_rows):
                m_name = grid[r][col_map["model"]]
                if not m_name or m_name.lower() in ["casting", "model", "tba", "photo"]: continue
                
                # Seri adını al ve TH/STH filtrele
                seri = grid[r][col_map.get("s_name", 0)] or ""
                if "treasure hunt" in seri.lower(): continue

                all_data.append({
                    "year": "2026",
                    "series": seri,
                    "series_num": grid[r][col_map.get("s_num", 0)],
                    "model_name": m_name,
                    "color": grid[r][col_map.get("color", 0)],
                    "toy_num": grid[r][col_map.get("toy", 0)],
                    "col_num": grid[r][col_map.get("col", 0)],
                    "image_url": images[r][col_map.get("photo", 0)],
                    "feature": "",
                    "scale": "1:64",
                    "category": "mainline"
                })

        # DataFrame oluştur ve CSV kaydet
        if all_data:
            df = pd.DataFrame(all_data)
            # Supabase'e atmadan önce sütunları temizle
            df = df.fillna("") # Boş değerleri temizle
            output = "hotwheels_2026_final_fixed.csv"
            df.to_csv(output, index=False, encoding="utf-8-sig")
            print(f"✅ İşlem tamam! {len(all_data)} araç hatasız kaydedildi: {output}")
        else:
            print("❌ Veri bulunamadı.")

    except Exception as e:
        print(f"❌ Hata: {e}")

if __name__ == "__main__":
    scrape_2026_mainline_v3_fixed()