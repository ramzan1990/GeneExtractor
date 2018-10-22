import h5py
import numpy as np

filename = "abundance.h5"
f = h5py.File(filename,'r')

#get estimated counts (ec), effective lengths (el) amd ids from h5 file
ec = f['est_counts'][()]
el = f["aux"]["eff_lengths"][()]
ids = f["aux"]["ids"][()]
size = len(ec)

#calculate tpm
rpk = np.zeros(shape=(size))
for i in range(size):
    rpk[i] = ec[i]/(el[i]*1000)
C = np.sum( rpk ) / 1000000
tpm = np.zeros(shape=(size))
for i in range(size):
    tpm[i] =  rpk[i] / C

f.close()

#get the gene mappings
geneID = {}
with open("mart_export2.txt", "r") as ins:
    for line in ins:
        a = line.split(",")
        geneID[a[1]] = a[0]

with open("mart_export.txt", "r") as ins:
    for line in ins:
        a = line.split()
        geneID[a[1]] = a[0]

#calculate estimated counts and tpm for each gene
IDValues = {}
for i in range(size):
    tid = ids[i].split(".")[0]
    if tid in geneID:
        cid = geneID[tid]
    else:
        cid = tid + "[not_found]"
    if cid in IDValues:
        IDValues[cid][0] = IDValues[cid][0] + ec[i]
        IDValues[cid][1] = IDValues[cid][1] + tpm[i]
    else: 
        IDValues[cid] = [ec[i], tpm[i]]

#write the result to a file
outF = open("result.csv", "w")
outF.write("ID")
outF.write(",")
outF.write("est_counts")
outF.write(",")
outF.write("tpm")
outF.write("\n")
for key in IDValues:
  outF.write(key)
  outF.write(",")
  outF.write(str(IDValues[key][0]))
  outF.write(",")
  outF.write(str(IDValues[key][1]))
  outF.write("\n")
outF.close()