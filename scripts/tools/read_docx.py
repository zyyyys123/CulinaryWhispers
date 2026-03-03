
import zipfile
import re
import sys
import os

def get_docx_text(path):
    if not os.path.exists(path):
        print(f"File not found: {path}")
        return

    try:
        with zipfile.ZipFile(path) as z:
            if 'word/document.xml' not in z.namelist():
                print("Not a valid docx (missing word/document.xml)")
                return
            
            xml_content = z.read('word/document.xml').decode('utf-8')
            
            # 1. Replace paragraph endings with newlines
            # <w:p> starts a paragraph, usually </w:p> ends it.
            # We can replace </w:p> with \n
            text = re.sub(r'</w:p>', '\n', xml_content)
            
            # 2. Replace <w:tab/> with \t
            text = re.sub(r'<w:tab/>', '\t', text)
            
            # 3. Remove all other XML tags
            text = re.sub(r'<[^>]+>', '', text)
            
            # 4. Unescape XML entities
            text = text.replace('&lt;', '<').replace('&gt;', '>').replace('&amp;', '&').replace('&quot;', '"').replace('&apos;', "'")
            
            print(text)
    except Exception as e:
        print(f"Error reading docx: {e}")

if __name__ == "__main__":
    # path = r"d:\MyCollegeProject\CulinaryWhispers\docs_main\2022005113-孔琳茜-任务书.docx"
    # Allow passing path as argument, strictly use the one provided by user
    if len(sys.argv) > 1:
        get_docx_text(sys.argv[1])
    else:
        print("Usage: python read_docx.py <path_to_docx>")
