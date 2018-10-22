import h5py
import numpy as np
import sys

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

#get the gene mappings from id_map.txt generated in step 1
geneID = {}
with open("id_map.txt", "r") as ins:
    for line in ins:
        a = line.split(",")        
        tid = a[1].strip()
        gid = a[0].strip()
        geneID[tid] = gid

#get the selected genes from step 1
genesStep1 = []
with open("out_step1.csv", "r") as ins:
    for line in ins:
        genesStep1.append(line.strip())

#calculate estimated counts and tpm for each gene
IDValues = {}
for i in range(size):
    tid = ids[i].split(".")[0]    
    if tid in geneID:
      cid = geneID[tid]
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
  #check if the gene was selected in step 1
  if(key in genesStep1):
    #check if the value is bigger then a cutoff parameter
    if(IDValues[key][1] > float(sys.argv[1])):
      outF.write(key)
      outF.write(",")
      outF.write(str(IDValues[key][0]))
      outF.write(",")
      outF.write(str(IDValues[key][1]))
      outF.write("\n")
outF.close()