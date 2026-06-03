import pandas as pd

def convert_to_excel(input_file, output_file):
    data = []

    with open(input_file, 'r', encoding='utf-8') as file:
        content = file.read()

    # Remove starting/ending braces if present
    content = content.strip().strip('{}')

    # Split into key-value pairs
    pairs = content.split(',')

    for pair in pairs:
        if '=' in pair:
            key, value = pair.split('=', 1)  # split only on first =
            key = key.strip()
            value = value.strip()

            data.append([key, value])

    # Create DataFrame
    df = pd.DataFrame(data, columns=['Key', 'Value'])

    # Save to Excel
    df.to_csv(output_file, index=False)

    print(f"Excel file created successfully: {output_file}")


# ✅ Example usage

input_file = r"C:\Users\vs85\Desktop\Misc\new 29.txt"
output_file = r"C:\Users\vs85\Desktop\Misc\output.csv"


# convert_to_excel(input_file, output_file)

