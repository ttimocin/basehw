import requests
from bs4 import BeautifulSoup
import json
import time
import re

def scrape_2026_matchbox_final():
    all_data = [] 
    base_api_url = "https://matchbox.fandom.com/api.php"
    page_title = "List_of_2026_Matchbox"
    headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"}

    print(f">>> {page_title} taranıyor (Category: superchase/mainline kuralı aktif)...")
    
    params = {
        "action": "parse", 
        "page": page_title, 
        "format": "json", 
        "prop": "text"
    }

    try:
        response = requests.get(base_api_url, params=params, headers=headers, timeout=30)
        data = response.json()
        
        if "error" in data:
            print(f"  ❌ Hata: {data['error'].get('info', 'Sayfa bulunamadı')}")
            return

        soup = BeautifulSoup(data["parse"]["text"]["*"], "html.parser")
        tables = soup.find_all("table", class_=lambda x: x and 'wikitable' in x)
        
        for table in tables:
            rows = table.find_all("tr")
            if not rows: continue

            # --- GRID SİSTEMİ (Kaymaları önlemek için) ---
            num_rows = len(rows)
            num_cols = 15
            grid = [[None for _ in range(num_cols)] for _ in range(num_rows)]
            images = [[None for _ in range(num_cols)] for _ in range(num_rows)]

            for r_idx, row in enumerate(rows):
                cells = row.find_all(["td", "th"])
                curr_c = 0
                for cell in cells:
                    while curr_c < num_cols and grid[r_idx][curr_c] is not None:
                        curr_c += 1
                    
                    rowspan = int(cell.get("rowspan", 1))
                    colspan = int(cell.get("colspan", 1))
                    content = cell.get_text(strip=True)
                    
                    img_tag = cell.find("img")
                    img_url = ""
                    if img_tag:
                        img_url = (img_tag.get("data-src") or img_tag.get("src", "")).split("/revision/")[0]

                    for r in range(rowspan):
                        for c in range(colspan):
                            if r_idx + r < num_rows and curr_c + c < num_cols:
                                grid[r_idx + r][curr_c + c] = content
                                if img_tag:
                                    images[r_idx + r][curr_c + c] = img_url
                    curr_c += colspan

            # Başlık Haritalama
            header = [str(x).lower() for x in grid[0]]
            col_map = {}
            for i, h in enumerate(header):
                if "model" in h or "name" in h: col_map["model_name"] = i
                elif "man #" in h or "man#" in h or "toy" in h: col_map["toy_num"] = i
                elif "series" in h: col_map["series"] = i
                elif "num" in h or "mbx#" in h: col_map["series_num"] = i
                elif "color" in h: col_map["color"] = i
                elif "photo" in h: col_map["photo"] = i

            if "model_name" not in col_map: continue

            for r in range(1, num_rows):
                m_name = grid[r][col_map["model_name"]]
                if not m_name or m_name.lower() in ["model", "name", "tba", "photo"]: continue
                
                # --- CATEGORY KURALLARI ---
                # Model isminde "Super Chase" varsa superchase, yoksa mainline
                cat_val = "mainline"
                if "super chase" in m_name.lower():
                    cat_val = "superchase"

                # --- GÖRSEL KURALLARI (Photo yoksa Color hücresindeki resmi al) ---
                final_img = images[r][col_map.get("photo", 0)] or images[r][col_map.get("color", 0)] or ""

                # Seri ve Numarayı Böl
                raw_series = grid[r][col_map.get("series", 0)] or "Matchbox Mainline"
                series_num = grid[r][col_map.get("series_num", 0)] or ""
                match = re.search(r'(.*?)(\d+/\d+)', raw_series)
                if match:
                    series_name = match.group(1).strip()
                    series_num = match.group(2).strip()
                else:
                    series_name = raw_series

                all_data.append({
                    "year": "2026",
                    "model_name": m_name,
                    "series": series_name,
                    "series_num": series_num,
                    "color": grid[r][col_map.get("color", 0)] or "",
                    "toy_num": grid[r][col_map.get("toy_num", 0)] or "",
                    "image_url": final_img,
                    "feature": "", # İsteğin üzerine boş bırakıldı
                    "category": cat_val, # superchase veya mainline burada
                    "scale": "1:64",
                    "data_source": "Matchbox_Fandom_2026"
                })

    except Exception as e:
        print(f"  ❌ Hata: {e}")

    # JSON Çıktısı
    if all_data:
        output_file = "matchbox_2026_final.json"
        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(all_data, f, ensure_ascii=False, indent=4)
        print(f"\n✅ İşlem Başarılı! {len(all_data)} araç kaydedildi.")
        print(f"🚀 Category sütununa 'superchase' ve 'mainline' atamaları yapıldı.")
    else:
        print("❌ Veri bulunamadı.")

if __name__ == "__main__":
    scrape_2026_matchbox_final()